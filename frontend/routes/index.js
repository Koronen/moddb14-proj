var express = require('express');
var router = express.Router();

/* GET home page. */
router.get('/', function(req, res) {
	res.redirect('/heatmap');
	/*
  res.render('index', { 
		title: 'Visualizing Github public activity on a heat map',
		subtitle: 'A project for the KTH course DD2471'
	});
	*/
});

/* GET Heatmap page */
router.get('/heatmap', function(req, res) {
	res.render('heatmap',{ 
		title: 'Visualizing Github public activity on a heat map',
		subtitle: 'A project for the KTH course DD2471'
		//authors: ["Simon", "Mitra", "Victor"]
	});
});

module.exports = router;
