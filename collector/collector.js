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

var sendRequest = function(callback) {
  var headers = {
    'Host': 'api.github.com',
    'User-Agent': 'moddb14-proj Collector (https://github.com/Koronen/moddb14-proj/collector)',
    'Accept': 'application/vnd.github.v3+json'
  };
  if(process.env.GITHUB_TOKEN) {
    headers.Authorization = "token " + process.env.GITHUB_TOKEN;
  }
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
  req.end();
};

MongoClient.connect(process.env.MONGODB_URL, function(err, db) {
  if (err) {
    throw err;
  }

  var processResponse = function(err, headers, body) {
    if(err) {
      throw err;
    }

    var events = JSON.parse(body);
    db.collection('collector').insert(events, function(err, records) {
      if (err) {
        throw err;
      }
    });
  };

  sendRequest(processResponse);
});
