"use strict";

var dotenv = require('dotenv');
var https = require('https');
var MongoClient = require('mongodb').MongoClient;

dotenv.load();
if(!process.env.MONGODB_URL || process.env.MONGODB_URL.length < 1) {
  console.error("Missing required environment variable MONGODB_URL!");
  process.exit(1);
}
if(!process.env.GITHUB_TOKEN || process.env.GITHUB_TOKEN.length < 1) {
  console.warn("Missing recommended environment variable GITHUB_TOKEN!");
}

var sendRequest = function(callback, extra_headers) {
  var headers = {
    'Host': 'api.github.com',
    'User-Agent': 'moddb14-proj Collector (https://github.com/Koronen/moddb14-proj/collector)',
    'Accept': 'application/vnd.github.v3+json'
  };
  for(var key in extra_headers) {
    headers[key] = extra_headers[key];
  };
  var options = {
    method: 'GET', hostname: 'api.github.com', port: 443, path: '/events', headers: headers
  };
  var req = https.request(options, function(res) {
    res.setEncoding('utf8');

    var body = '';
    res.on('data', function (chunk) {
      body += chunk;
    });
    res.on('end', function () {
      callback(null, res.headers, body);
    });
  });
  req.on('error', function(e) {
    callback(e, null, null);
  });
  console.log(new Date() + ": Sending request to GitHub");
  req.end();
};

var extra_headers = {};
if(process.env.GITHUB_TOKEN) {
  extra_headers.Authorization = "token " + process.env.GITHUB_TOKEN;
}

var processResponse = function(err, headers, body) {
  var delay = 1000;

  if (err) {
    console.error(new Date() + ": " + err);
  }
  else {
    if(headers['status'] === '200 OK') {
      extra_headers['If-None-Match'] = headers['etag'];
      extra_headers['If-Modified-Since'] = headers['last-modified'];
      delay = headers['x-poll-interval'] * 1000;

      processEvents(JSON.parse(body));
    }
    else if(headers['status'] === '304 Not Modified') {
      // No-op
    }
    else if(headers['status'] === '403 Forbidden' &&
      headers['x-ratelimit-remaining'] === '0') {
      console.warn(new Date() + ": Hit rate limit!");
      delay = headers['x-ratelimit-reset']*1000-Date.now();
    }
    else {
      console.warn(new Date() + ": Unexpected response status \"" + headers['status'] + "\"")
    }
  }

  console.log(new Date() + ": Sending next request in " + delay/1000 + "s")
  setTimeout(function() {
    sendRequest(processResponse, extra_headers);
  }, delay);
};
sendRequest(processResponse, extra_headers);

var processEvents = function(events) {
  MongoClient.connect(process.env.MONGODB_URL, function(err, db) {
    if (err) {
      console.error(new Date() + ": " + err);
      process.exit(1);
    }

    db.ensureIndex(
      'collector',
      { id: 1 },
      { unique: true, background: true, dropDups: true },
      function(err, indexName) {
        if(err) {
          console.error(new Date() + ": " + err);
          process.exit(1);
        }

        db.collection('collector').insert(events, function(err, records) {
          if (err) {
            console.error(new Date() + ": " + err);
            process.exit(1);
          }
          else {
            console.log(new Date() + ": Inserted " + records.length + " events");
          }
        });
      }
    );
  });
};
