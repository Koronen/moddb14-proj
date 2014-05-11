# moddb14-proj collector

Listens for data from the [GitHub Events API][GitHubEventAPI], filters out
events considered to be contributions and adds them to a RabbitMQ message queue.
Also fetches the users' locations from the [GitHub Users API][GitHubUsersAPI],
caches them in Redis and passes them along with the events.

[GitHubEventAPI]: https://developer.github.com/v3/activity/events/
[GitHubUsersAPI]: https://developer.github.com/v3/users/

## How to install

    ./script/bootstrap

## How to configure

    $EDITOR .env

## How to run

    npm start
