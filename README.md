# OpenBaton
[![Build Status](https://travis-ci.org/openbaton/NFVO.svg?branch=master)](https://travis-ci.org/openbaton/NFVO)
[![Join the chat at https://gitter.im/openbaton/NFVO](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/openbaton/NFVO?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

OpenBaton is an open source project providing a reference implementation of the NFVO and VNFM based on the ETSI [NFV MANO] specification. 

## Getting Started

This `VNFM` is implemented in java using the [spring.io] framework.

For using this VNFM you have to install the NFVO and start it. How to do this can be found [here][nfvo install].

## install the latest MS-VNFM version from the source code

1. Download the source code by using git:

```bash
git clone https://gitlab.tubit.tu-berlin.de/NUBOMEDIA/ms-vnfm.git
```

2. Compile the code by using gradle

```bash
cd ms-vnfm
./gradlew build
```

3. Start the VNFManager

```bash
java -jar build/libs/ms-vnfm-{VERSION}.jar
```

The VNFM uses the RabbitMQ for communicating with the NFVO. Therefore it is a prerequisites to have RabbitMQ up and running.

## Configuration

The configuration can be found in `src/main/resources/application.properties`.

Here you can configure:
 * NFVO
 * Database
 * RabbitMQ
 * Log levels

After chaning any configuration, you need to recompile the code.



[nfvo install]: http://openbaton.github.io/documentation/nfvo-installation/
[spring.io]:https://spring.io/
[NFV MANO]:http://www.etsi.org/deliver/etsi_gs/NFV-MAN/001_099/001/01.01.01_60/gs_nfv-man001v010101p.pdf
[openbaton]:http://twitter.com/openbaton
[website]:http://www.open-baton.org
