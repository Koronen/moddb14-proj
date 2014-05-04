var express = require('express');
var router = express.Router();

/* GET home page. */
router.get('/', function(req, res) {

  res.render('index', { title: 'Express' });
});

/* GET Heatmap page */
router.get('/heatmap', function(req, res) {
	// FIXME Can't render directly
	/*
	var db = req.db;
	var collection = db.get('ccoll');
	collection.find({},{},function(e,docs){
		if(e){
			console.log(e);
		}
		console.log("Initial\n" + docs);
		res.render('heatmap', {
				"dbdata" : docs
		});
	});
	*/
	res.render('heatmap',{});
});

module.exports = router;
