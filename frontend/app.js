var express = require('express');
var path = require('path');
var favicon = require('static-favicon');
var logger = require('morgan');
var cookieParser = require('cookie-parser');
var bodyParser = require('body-parser');

var mongo = require('mongodb');
var monk = require('monk');
var db = monk('localhost:27017/moddb');

var routes = require('./routes/index');
var users = require('./routes/users');

var app = express();

var server = require("http").createServer(app);
var io = require('socket.io').listen(server);


// view engine setup
app.set('views', path.join(__dirname, 'views'));
app.set('view engine', 'ejs');

app.use(favicon());
app.use(logger('dev'));
app.use(bodyParser.json());
app.use(bodyParser.urlencoded());
app.use(cookieParser());
app.use(express.static(path.join(__dirname, 'public')));

// Make our db accessible to our router
app.use(function(req,res,next){
    req.db = db;
    next();
});
//app.use(express.static(process.cwd() + '/public'));
app.use('/', routes);
app.use('/users', users);

/// catch 404 and forwarding to error handler
app.use(function(req, res, next) {
    var err = new Error('Not Found');
    err.status = 404;
    next(err);
});

/// error handlers

// development error handler
// will print stacktrace
if (app.get('env') === 'development') {
    app.use(function(err, req, res, next) {
        res.status(err.status || 500);
        res.render('error', {
            message: err.message,
            error: err
        });
    });
}

// production error handler
// no stacktraces leaked to user
app.use(function(err, req, res, next) {
    res.status(err.status || 500);
    res.render('error', {
        message: err.message,
        error: {}
    });
});

io.sockets.on("connection", function(socket){
	socket.on("dump", function(data){
		console.log(data);
	});
	socket.on("fetch data", function(data){
		var collection = db.get('ccoll');
    collection.find({},{},function(e,docs){
			if(e){
				console.log(e);
			}
			var maxvalue = 0;
			//var sumvalue = 0;
			docs.forEach(function(entry){
				//sumvalue += entry.value;
				maxvalue = (maxvalue >= entry.value ? maxvalue : entry.value);
			});
			
			var datamap = {}
			docs.forEach(function(entry){
				datamap[entry.cid] = entry.value/maxvalue;
			});
			
			socket.emit("db update", datamap)
    });
	});
});

server.listen(3001);
module.exports = app;
