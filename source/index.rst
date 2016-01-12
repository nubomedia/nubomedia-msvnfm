.. Elastic Media Manager documentation master file, created by
   sphinx-quickstart on Tue Jan  5 17:26:28 2016.
   You can adapt this file completely to your liking, but it should at least
   contain the root `toctree` directive.

Welcome to Elastic Media Manager's documentation!
=================================================

This project is part of NUBOMEDIA prject: [nubomedia.eu]_

This `VNF Manager`, called `ms-vnfm`, is implemented in java using the [spring.io]_ framework.

For using this VNFM you have to install the NFVO and start it. How to do this can be found [nfvo_install]_.

The VNFM uses the RabbitMQ for communicating with the NFVO. Therefore it is a prerequisites to have RabbitMQ up and running.
This is done automatically by executing the bootstrap script of the NFVO.

install the latest MS-VNFM version from the source code
-------------------------------------------------------

You can install and start the ms-vnfm either automatically by downloading and executing the bootstrap or manually.
Both options are described below.

Install and start it automatically
----------------------------------

This repository [bootstrap]_ contains the bootstrap script to install and start the ms-vnfm automatically.

In order to install and start the ms-vnfm you can run the following command:

```bash
curl -fsSkL https://github.com/tub-nubomedia/bootstrap/raw/master/bootstrap | bash
```

Afterwards the source code of the ms-vnfm is located in `/opt/nubomedia/ms-vnfm`.

**Note** It is expected that the NFVO is already installed and started. Otherwise the ms-vnfm will wait for 600s to register to the NFVO. Once this time is passed you need to start the ms-vnfm manually when the NFVO is up and running.

Install the ms-vnfm manually
----------------------------

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
Start the ms-vnfm manually
---------------------------

the ms-vnfm can be started by executing the following command:

```bash
./ms-vnfm.sh start
```

Once the ms-vnfm is started you can access the screen session with:

```bash
screen -r nubomedia
```

Configuration
--------------

The configuration can be found in `/opt/nubomedia/ms-vnfm/src/main/resources/application.properties`.

Here you can configure:
    * NFVO
    * Database
    * RabbitMQ
    * Log levels

After changing any configuration, you need to recompile the code.

Logging
------------

The log file is located in `/var/log/nubomedia/ms-vnfm.log`.


LICENSE
-------

See the `LICENSE. <https://raw.githubusercontent.com/tub-nubomedia/ms-vnfm/master/LICENSE>`_

.. [bootstrap] https://github.com/tub-nubomedia/bootstrap
.. [nfvo_install] http://openbaton.github.io/documentation/nfvo-installation/
.. [spring.io] https://spring.io/
.. [nubomedia.eu] http://www.nubomedia.eu/

.. toctree::
   :maxdepth: 2

.. Indices and tables
   * :ref:`genindex`
   * :ref:`modindex`
   * :ref:`search`


