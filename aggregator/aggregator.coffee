dotenv = require 'dotenv'
amqp = require 'amqp'
MongoClient = require('mongodb').MongoClient

dotenv.load()
if !process.env.AMQP_URL or process.env.AMQP_URL.length < 1
  console.error "Missing required environment variable AMQP_URL!"
  process.exit 1
if !process.env.AMQP_QUEUE_NAME or process.env.AMQP_QUEUE_NAME.length < 1
  console.error "Missing required environment variable AMQP_QUEUE_NAME!"
  process.exit 1
if !process.env.MONGODB_URL or process.env.MONGODB_URL.length < 1
  console.error "Missing required environment variable MONGODB_URL!"
  process.exit 1

MongoClient.connect process.env.MONGODB_URL, (err, db) ->
  if err
    console.log err
    process.exit 1

  collection = db.collection 'events'

  connection = amqp.createConnection url: process.env.AMQP_URL
  connection.on 'ready', ->
    connection.queue process.env.AMQP_QUEUE_NAME, { passive: true }, (queue) ->
      queue.subscribe (msg) ->
        event = JSON.parse msg.data.toString('utf-8')

        minified_event =
          actor: event.actor.country_iso
          created_at: new Date(event.created_at)

        collection.insert minified_event, (err, docs) ->
          if err
            console.log err
            process.exit 1

  connection.on 'error', (err) ->
    console.error err
    process.exit 1
