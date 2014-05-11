# moddb14-proj

Project for the course "DD2471: Modern Database Systems and Their Applications"
at KTH.

## Dependencies

This project depends on Node.js, RabbitMQ, Java, Apache Ant, Redis and MongoDB.
For installation instructions, see the corresponding heading below.

### Debian Wheezy

First of all, ensure that `wheezy-backports` is enabled in APT. If not run the
following commands as root to enable it.

    echo "deb http://ftp.se.debian.org/debian wheezy-backports main" >> /etc/apt/sources.list
    echo "deb-src http://ftp.se.debian.org/debian wheezy-backports main" >> /etc/apt/sources.list
    apt-get update

Then run the following command as root to install all dependencies.

    apt-get install nodejs nodejs-legacy curl rabbitmq-server default-jdk ant \
        redis-server mongodb-server
    curl https://www.npmjs.org/install.sh | sh
