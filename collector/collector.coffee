dotenv = require 'dotenv'
https = require 'https'
amqp = require 'amqp'

dotenv.load()
if !process.env.AMQP_URL or process.env.AMQP_URL.length < 1
  console.error "Missing required environment variable AMQP_URL!"
  process.exit 1

if !process.env.GITHUB_TOKEN or process.env.GITHUB_TOKEN.length < 1
  console.warn "Missing recommended environment variable GITHUB_TOKEN!"

class GitHubClient
  constructor: (token, callback) ->
    @token = token
    @callback = callback
    @etag = undefined
    @lastModified = undefined

  start: ->
    @sendRequest()

  sendRequest: ->
    headers = {
      'Host': 'api.github.com',
      'User-Agent': 'moddb14-proj Collector (https://github.com/Koronen/moddb14-proj/collector)',
      'Accept': 'application/vnd.github.v3+json'
    }
    if @token
      headers['Authorization'] = "token #{@token}"
    if @etag
      headers['If-None-Match'] = @etag
    if @lastModified
      headers['If-Modified-Since'] = @lastModified

    options = {
      method: 'GET', hostname: 'api.github.com', port: 443, path: '/events', headers: headers
    }
    req = https.request options, (res) =>
      res.setEncoding 'utf8'

      body = ''
      res.on 'data', (chunk) ->
        body += chunk
      res.on 'end', =>
        @processResponse null, { headers: res.headers, body: body }
    req.on 'error', (e) =>
      @processResponse e, null
    console.log new Date() + ": Sending request to GitHub"
    req.end()

  processResponse: (err, response) ->
    delay = 1000

    if err
      console.error new Date() + ": " + err
    else
      headers = response.headers
      body = response.body

      if headers['status'] is '200 OK'
        @etag = headers['etag']
        @lastModified = headers['last-modified']
        delay = headers['x-poll-interval'] * 1000

        @callback(JSON.parse(body))
      else if headers['status'] is '304 Not Modified'
        # No-op
      else if headers['status'] is '403 Forbidden' and headers['x-ratelimit-remaining'] is '0'
        console.warn new Date() + ": Hit rate limit!"
        delay = headers['x-ratelimit-reset']*1000-Date.now()
      else
        console.warn new Date() + ": Unexpected response status \"" + headers['status'] + "\""

    console.log new Date() + ": Sending next request in " + delay/1000 + "s"
    setTimeout =>
      @sendRequest
    , delay

client = new GitHubClient process.env.GITHUB_TOKEN, (events) ->
  connection = amqp.createConnection url: process.env.AMQP_URL
  connection.on 'ready', ->
    connection.publish('raw-events', event) for event in events

  connection.on 'error', (err) ->
    console.error err
    process.exit 1
client.start()
