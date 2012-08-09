whistle
=======

Whistle is an implementation of the Reverse Causal Reasoning (RCR) algorithm on the `OpenBEL`_ platform.

Building
--------

Whistle is built in Java |trade| using `Apache Maven`_.

To build Whistle with `Apache Maven`_ type::

  mvn package assembly:single

The whistle distribution zip file is then located at::

  target/whistle-1.0-distribution.zip

Running
-------

Whistle ships with a default BEL Framework configuration that should work for most use cases.  If you want to reuse an existing installation of the BEL Framework then set the appropriate environment variable.

Linux or OS X::

  export BELFRAMEWORK_HOME=/path/to/bel/framework

Windows::

  set BELFRAMEWORK_HOME=c:\path\to\bel\framework

To run Whistle extract the distribution and run:

Linux or OS X::

  ./whistle.sh --help

Windows::

  whistle.cmd --help

You must accept the License terms to run it.

For additional information on running Whistle, please refer to the `Wiki`_.

.. |trade|   unicode:: U+2122 .. TRADEMARK
.. _OpenBEL: https://github.com/OpenBEL
.. _Apache Maven: http://maven.apache.org
.. _Wiki: https://github.com/Selventa/whistle/wiki
