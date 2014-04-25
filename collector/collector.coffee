dotenv = require 'dotenv'
https = require 'https'
amqp = require 'amqp'

dotenv.load()
if !process.env.AMQP_URL or process.env.AMQP_URL.length < 1
  console.error "Missing required environment variable AMQP_URL!"
  process.exit 1

if !process.env.GITHUB_TOKEN or process.env.GITHUB_TOKEN.length < 1
  console.warn "Missing recommended environment variable GITHUB_TOKEN!"

sendRequest = (callback, extra_headers) ->
  headers = {
    'Host': 'api.github.com',
    'User-Agent': 'moddb14-proj Collector (https://github.com/Koronen/moddb14-proj/collector)',
    'Accept': 'application/vnd.github.v3+json'
  }
  for key in extra_headers
    headers[key] = extra_headers[key]
  options = {
    method: 'GET', hostname: 'api.github.com', port: 443, path: '/events', headers: headers
  }
  req = https.request options, (res) ->
    res.setEncoding 'utf8'

    body = ''
    res.on 'data', (chunk) ->
      body += chunk
    res.on 'end', ->
      callback null, res.headers, body
  req.on 'error', (e) ->
    callback(e, null, null)
  console.log new Date() + ": Sending request to GitHub"
  req.end()

extra_headers = {}
if process.env.GITHUB_TOKEN
  extra_headers.Authorization = "token #{process.env.GITHUB_TOKEN}"

processResponse = (err, headers, body) ->
  delay = 1000

  if err
    console.error new Date() + ": " + err
  else
    if headers['status'] is '200 OK'
      extra_headers['If-None-Match'] = headers['etag']
      extra_headers['If-Modified-Since'] = headers['last-modified']
      delay = headers['x-poll-interval'] * 1000

      processEvents(JSON.parse(body))
    else if headers['status'] is '304 Not Modified'
      # No-op
    else if headers['status'] is '403 Forbidden' and headers['x-ratelimit-remaining'] is '0'
      console.warn new Date() + ": Hit rate limit!"
      delay = headers['x-ratelimit-reset']*1000-Date.now()
    else
      console.warn new Date() + ": Unexpected response status \"" + headers['status'] + "\""

  console.log new Date() + ": Sending next request in " + delay/1000 + "s"
  setTimeout ->
    sendRequest processResponse, extra_headers
  , delay
sendRequest processResponse, extra_headers

processEvents = (events) ->
  connection = amqp.createConnection url: process.env.AMQP_URL
  connection.on 'ready', ->
    connection.publish('raw-events', event) for event in events

  connection.on 'error', (err) ->
    console.error err
    process.exit 1
