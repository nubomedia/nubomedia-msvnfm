# MS-VNFM
<!--
[![Build Status](https://travis-ci.org/openbaton/NFVO.svg?branch=master)](https://travis-ci.org/openbaton/NFVO)
[![Join the chat at https://gitter.im/openbaton/NFVO](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/openbaton/NFVO?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
-->

This project is part of NUBOMEDIA prject: [nubomeda](https://www.nubomedia.eu "NUBOMEDIA")

## Getting Started

This `VNF Manager`, called `ms-vnfm`, is implemented in java using the [spring.io] framework.

For using this VNFM you have to install the NFVO and start it. How to do this can be found [here][nfvo install].

The VNFM uses the RabbitMQ for communicating with the NFVO. Therefore it is a prerequisites to have RabbitMQ up and running.
This is done automatically by executing the bootstrap script of the NFVO.

## install the latest MS-VNFM version from the source code

You can install and start the ms-vnfm either automatically by downloading and executing the bootstrap or manually.
Both options are described below.

### Install and start it automatically

This [repository][bootstrap] contains the bootstrap script to install and start the ms-vnfm automatically.

In order to install and start the ms-vnfm you can run the following command:

```bash
curl -fsSkL https://github.com/tub-nubomedia/bootstrap/raw/master/bootstrap | bash
```

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

The configuration can be found in `/opt/nubomedia/ms-vnfm/src/main/resources/application.properties`.

Here you can configure:
 * NFVO
 * Database
 * RabbitMQ
 * Log levels

After changing any configuration, you need to recompile the code.

## Logging

The log file is located in `/var/log/nubomedia/ms-vnfm.log`.

## LICENSE

See [LICENSE][LICENSE]

[bootstrap]:https://github.com/tub-nubomedia/bootstrap
[nfvo install]:http://openbaton.github.io/documentation/nfvo-installation/
[spring.io]:https://spring.io/
[NFV MANO]:http://www.etsi.org/deliver/etsi_gs/NFV-MAN/001_099/001/01.01.01_60/gs_nfv-man001v010101p.pdf
[LICENSE]:./LICENSE
