var dotenv = require('dotenv');
dotenv.load();

var MongoClient = require('mongodb').MongoClient;
MongoClient.connect(process.env.MONGODB_URL, function(err, db) {
  if (err) {
    throw err;
  }
  console.log("Connected to Database");

  var document = { user: { location: "Stockholm, Sweden" } };

  db.collection('collector').insert(document, function(err, records) {
    if (err) {
      throw err;
    }
    console.log("Record added as " + records[0]._id);
  });
});
