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
  constructor: (token) ->
    @token = token
    @caching = {}

  sendRequest: (path, callback) ->
    headers = {
      'Host': 'api.github.com',
      'User-Agent': 'moddb14-proj Collector (https://github.com/Koronen/moddb14-proj/collector)',
      'Accept': 'application/vnd.github.v3+json'
    }
    if not @caching[path]
      @caching[path] = {}
    if @token
      headers['Authorization'] = "token #{@token}"
    if @caching[path]['etag']
      headers['If-None-Match'] = @caching[path]['etag']
    if @caching[path]['last-modified']
      headers['If-Modified-Since'] = @caching[path]['last-modified']

    options = {
      method: 'GET', hostname: 'api.github.com', port: 443, path: path, headers: headers
    }
    req = https.request options, (res) =>
      res.setEncoding 'utf8'

      body = ''
      res.on 'data', (chunk) ->
        body += chunk
      res.on 'end', =>
        if res.headers['status'] is '200 OK'
          @caching[path] = { 'etag': headers['etag'], 'last-modified': headers['last-modified'] }

        callback res.headers, body
    req.on 'error', (e) ->
      console.error new Date() + ": " + err
    console.log new Date() + ": Sending #{path} request to GitHub"
    req.end()

  processResponse: (response, callback) ->
    headers = response.headers
    body = response.body
    delay = 1000

connection = amqp.createConnection url: process.env.AMQP_URL
connection.on 'ready', ->
  client = new GitHubClient process.env.GITHUB_TOKEN

  pollEvents = ->
    client.sendRequest '/events', (headers, body) ->
      delay = 60000

      if headers['status'] is '200 OK'
        delay = headers['x-poll-interval'] * 1000

        events = JSON.parse(body)
        for event in events
          addLocation event, (eventWithLocation) ->
            connection.publish('raw-events', eventWithLocation)

      else if headers['status'] is '403 Forbidden' and headers['x-ratelimit-remaining'] is '0'
        console.warn new Date() + ": Hit rate limit!"
        delay = headers['x-ratelimit-reset']*1000-Date.now()
      else
        console.warn new Date() + ": Unexpected response status \"#{headers['status']}\""

      console.log new Date() + ": Sending next request in " + delay/1000 + "s"
      setTimeout pollEvents, delay

  loginToLocation = {}
  addLocation = (event, callback) ->
    login = event.actor.login
    if loginToLocation[login]
      event.actor.location = loginToLocation[login]
      callback event
    else
      client.sendRequest "/users/#{login}", (headers, body) ->
        location = JSON.parse(body).location
        event.actor.location = location
        loginToLocation[login] = location
        console.log new Date() + ": Set #{login} location to \"#{location}\""
        callback event

  pollEvents()

connection.on 'error', (err) ->
  console.error err
  process.exit 1
