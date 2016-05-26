# MS-VNFM
<!--
[![Build Status](https://travis-ci.org/openbaton/NFVO.svg?branch=master)](https://travis-ci.org/openbaton/NFVO)
[![Join the chat at https://gitter.im/openbaton/NFVO](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/openbaton/NFVO?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
-->

This project is part of NUBOMEDIA project: [NUBOMEDIA][nubomedia.eu]

## Welcome to Elastic Media Manager's documentation!

This `VNF Manager`, called `ms-vnfm` also known as `emm`, is implemented in java using the [spring.io] framework.

For using this VNFM you have to install the NFVO and start it. How to do this can be found [here][nfvo install].

The VNFM uses the RabbitMQ for communicating with the NFVO. Therefore it is a prerequisites to have RabbitMQ up and running.
This is done automatically by executing the bootstrap script of the NFVO.

## install the latest MS-VNFM version from the source code

You can install and start the ms-vnfm either automatically by downloading and executing the bootstrap or manually.
Both options are described below.

### Install and start it automatically

This repository contains a [bootstrap](boostrap) script to install and start the ms-vnfm automatically.

In order to install and start the ms-vnfm you can run the following command:

```bash
bash <(curl -fSskL https://raw.githubusercontent.com/tub-nubomedia/ms-vnfm/master/bootstrap)
```

During the bootstrap process you are requested for setting default configurations. A list of configuration parameters and their meaning can be found [here](#configuration)

Afterwards the source code of the ms-vnfm is located in `/opt/nubomedia/ms-vnfm`.

**Note** It is expected that the NFVO is already installed and started. Otherwise the ms-vnfm will wait for 600s to register to the NFVO. Once this time is passed you need to start the ms-vnfm manually when the NFVO is up and running.

In case the ms-vnfm is already installed you can start the ms-vnfm manually by using the provided script as described [here](#start-the-ms-vnfm-manually)

### Install the ms-vnfm manually

1. Download the source code by using git:

```bash
git clone https://github.com/tub-nubomedia/ms-vnfm.git
```

This command will clone the git repository to the folder `ms-vnfm`

2. Compile the code by using the provided script

```bash
cd ms-vnfm
./ms-vnfm.sh compile
```

3. Configuration

After compiling the code, you have to configure default parameters in the configuration file. More on this topic can be found [here](#configuration)

### Start the ms-vnfm manually

the ms-vnfm can be started by executing the following command:

```bash
./ms-vnfm.sh start
```

Once the ms-vnfm is started you can access the screen session with:

```bash
screen -r nubomedia
```

## Configuration

The configuration can be found in `/etc/nubomedia/msvnfm.properties`. A default configuration file can be found [here](vnfm-configuration-file)

Here you can configure:
 * NFVO
 * Database
 * RabbitMQ
 * Log levels
 * Application
 * MediaServer
 * AutoScaling

After changing any configuration, just restart the VNFM.

The following list gives an overview of available configuration parameters and its meaning:
* logging.*: Log-related settings can be done here.
    * logging.file: This defines the log location.
    * logging.level.*: Log levels can be configured with this parameter. Just define the package and the level of log you want to have, like: logging.level.org.openbaton=INFO. In this way you can add log levels for specfic packages or you can configure existing levels.
* vnfm.*: This prefix configures VNFM-related properties.
    * vnfm.rabbitmq.*: This prefix configures VNFM RabbitMQ-related properties.
        * vnfm.rabbitmq.brokerIp: This is the IP of the RabbitMQ broker.
        * vnfm.rabbitmq.management.port: This defines the management port of the RabbitMQ (default: 15672).
        * vnfm.rabbitmq.minConcurrency: This defines the minimum number of VNFMs running concurrently.
        * vnfm.rabbitmq.maxConcurrency: This defines the maximum number of VNFMs running concurrently.
    * vnfm.server.port: This is the port of the exposed API, e.g., un/register new Applications, querying MediaServers or VNFRs.
    * vnfm.management.port: This is the management Port of the exposed API.
* spring.*: This prefix configures Spring-related properties. A full list of predefined properties of Spring can be found [here](spring-properties).
    * spring.datasource.*: Here you can define specific properties for the Database.
        * spring.datasource.url: JDBC url of the database. This defines the URL where the database is available. In the configuration file you can choose between MySQL and HSQL.
        * spring.datasource.driver-class-name: This configures the database driver that is used for the communication between the application and the choosen type of database. Fully qualified name of the JDBC driver. Auto-detected based on the URL by default.
    * spring.jpa.*: Spring Data JPA is responsible for storing and retrieving data in a relational database.
        * spring.jpa.database-platform: Name of the target database to operate on, auto-detected by default. Can be alternatively set using the "Database" enum.
        * spring.jpa.show-sql: Enable logging of SQL statements.
        * spring.jpa.hibernate.ddl-auto: DDL mode. This is actually a shortcut for the "hibernate.hbm2ddl.auto" property. Default to "create-drop" when using an embedded database, "none" otherwise.
    * spring.rabbitmq.*: Here you can set RabbitMQ-related properties from Spring-side.
        * spring.rabbitmq.host: This defines the location (IP) of the RabbitMQ host.
        * spring.rabbitmq.port: This defines the RabbitMQ port (default: 5672)
        * spring.rabbitmq.username: Username to authenticate to the broker.
        * spring.rabbitmq.password: Password for the defined user to authenticate against the broker.
        * spring.rabbitmq.listener.concurrency: Defines the minimum number of consumers.
        * spring.rabbitmq.listener.max-concurrency: Defines the maxmimum number of consumers.
* autoscaling.*:
    * autoscaling.monitor.url: This defines the monitoring url used by the AutoScaling system (default: localhost). Monitoring information are provided by the VNFM itself (available: CONSUMED_CAPACITY).
    * autoscaling.pool.*: Here you can define pool-related properties. The pool is used to prepare VNFCInstances that can be fetched during scale-out to avoid launching and instantiation times when creating new VMs.
        * autoscaling.pool.activate: Enables the pool mechanism. If this is activated (`true`), the AutoScaling system will prepare VNFCInstances of the defined number for each VDU where the VNFR contains AutoScalePolicies.
        * autoscaling.pool.size: This defines the number of VNFCInstance that will be prepared for each VDU.
        * autoscaling.pool.period: This defines the period that the pool mechanism will use to check the number of reserved VNFCInstances. In case the current pool size is lower than the defined pool size, it will launch the number of Instances that are missing to fill the pull.
        * autoscaling.pool.prepare: If `true`, VNFCInstances for the pool will be prepared when starting the VNFR. If `false`, VNFCInstances will be prepared once the VNFR went to ACTIVE, so after starting the VNFR.
    * autoscaling.termination-rule.*: This defines a Termination rule that is considered while scaling-in. If this termination rule does not meet, the VNFCInstance will not be removed at all. Main scenario is that scaling-in is allowed only when no more sessions are running on that VNFCInstance.
        * autoscaling.termination-rule.activate: `true`, it this termination rule should be activated.
        * autoscaling.termination-rule.metric: This defines the name of the metric that is checked when scaling-in.
        * autoscaling.termination-rule.value: This defines the value that must be meet for allowing scale-in this VNFCInstance.
* mediaserver.*: This defines MediaServer-related properties.
    * mediaserver.capacity.max: This defines the maximum capacity that every VNFCInstance provides for establishing applications.
    * mediaserver.monitor.url: This url is the monitoring URL of the MediaServers. It is used basically for checking termination rules, if activated, when scaling-in. This monitor should provide the number of session that are allocated on a specific MediaServer.
    * mediaserver.stun-server.*: This configures STUNServer-related properties.
        * mediaserver.stun-server.activate: `true` if activated by default. `false` if not.
        * mediaserver.stun-server.address: This defines the IP of the STUN-Server to use.
        * mediaserver.stun-server.port: This defines the port of the STUN-Server to use.
    * mediaserver.turn-server.*: This configures TURNServer-related properties.
        * mediaserver.turn-server.activate: `true` if activated by default. `false` if not.
        * mediaserver.turn-server.url: This defines the URL of the TURN-Server to use.
        * mediaserver.turn-server.username: This defines the username to authenticate to the TURN-Server.
        * mediaserver.turn-server.password: This defines the password to authenticate against the TURN-Server.
* application.*: This configures Application-related properties.
    * application.heartbeat.*: Here you can configurate Heartbeat-realted properties of an Application.
        * application.heartbeat.activate: `true` if the Heartbeat-mechanism should be activated. `false` if not.
        * application.heartbeat.period: This defines the period of checking Heartbeats. If no Heartbeat was send in this period, it will increase the missing Heatbeat-counter by one.
        * application.heartbeat.retry.*: Configures retry configration when a Heartbeat was missed.
            * application.heartbeat.retry.max: This defines the maximum number of reties before removing the Application.
            * application.heartbeat.retry.timeout: If this timeout (in s) is passed without receiving any Heartbeat for a specific Application, it will be removed.
* nfvo.*: This configures NFVO-related properties.
    * nfvo.ip: This property defines the IP of the NFVO.
    * nfvo.port: This property defines the port of the NFVO.
    * nfvo.username: This property defines the username used for authentication to the NFVO.
    * nfvo.password: This property defines the password user for authentication against the NFVO.

## Logging

The log file is located in `/var/log/nubomedia/ms-vnfm.log` by default. The path can be changed in `/etc/nubomedia/msvnfm.properties` adapting the property `logging.file=` to your needs.

News
----

Follow us on Twitter @[NUBOMEDIA Twitter].

Issue tracker
-------------

Issues and bug reports should be posted to [GitHub Issues].

Licensing and distribution
--------------------------

Software associated to NUBOMEDIA is provided as open source under GNU Library or
"Lesser" General Public License, version 2.1 (LGPL-2.1). Please check the
specific terms and conditions linked to this open source license at
http://opensource.org/licenses/LGPL-2.1. Please note that software derived as a
result of modifying the source code of NUBOMEDIA software in order to fix a bug
or incorporate enhancements is considered a derivative work of the product.
Software that merely uses or aggregates (i.e. links to) an otherwise unmodified
version of existing software is not considered a derivative work.

Contribution policy
-------------------

You can contribute to the NUBOMEDIA community through bug-reports, bug-fixes,
new code or new documentation. For contributing to the NUBOMEDIA community,
drop a post to the [NUBOMEDIA Public Mailing List] providing full information
about your contribution and its value. In your contributions, you must comply
with the following guidelines

* You must specify the specific contents of your contribution either through a
  detailed bug description, through a pull-request or through a patch.
* You must specify the licensing restrictions of the code you contribute.
* For newly created code to be incorporated in the NUBOMEDIA code-base, you
  must accept NUBOMEDIA to own the code copyright, so that its open source
  nature is guaranteed.
* You must justify appropriately the need and value of your contribution. The
  NUBOMEDIA project has no obligations in relation to accepting contributions
  from third parties.
* The NUBOMEDIA project leaders have the right of asking for further
  explanations, tests or validations of any code contributed to the community
  before it being incorporated into the NUBOMEDIA code-base. You must be ready
  to addressing all these kind of concerns before having your code approved.

Support
-------

The NUBOMEDIA community provides support through the [NUBOMEDIA Public Mailing List].

[Development Guide]: http://nubomedia.readthedocs.org/
[GitHub Issues]: https://github.com/tub-nubomedia/marketplace/issues
[GitHub NUBOMEDIA Group]: https://github.com/nubomedia
[LGPL v2.1 License]: http://www.gnu.org/licenses/lgpl-2.1.html
[NUBOMEDIA Logo]: http://www.nubomedia.eu/sites/default/files/nubomedia_logo-small.png
[NUBOMEDIA Twitter]: https://twitter.com/nubomedia
[NUBOMEDIA Public Mailing list]: https://groups.google.com/forum/#!forum/nubomedia-dev
[NUBOMEDIA]: http://www.nubomedia.eu
[nubomedia.eu]: http://www.nubomedia.eu/
[bootstrap]:https://raw.githubusercontent.com/tub-nubomedia/ms-vnfm/master/bootstrap
[nfvo install]:http://openbaton.github.io/documentation/nfvo-installation/
[spring.io]:https://spring.io/
[NFV MANO]:http://www.etsi.org/deliver/etsi_gs/NFV-MAN/001_099/001/01.01.01_60/gs_nfv-man001v010101p.pdf
[LICENSE]:./LICENSE
[vnfm-configuration-file]:https://github.com/tub-nubomedia/ms-vnfm/blob/master/etc/msvnfm.properties
[spring-properties]:http://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#common-application-properties