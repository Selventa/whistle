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

To run Whistle extract the distribution and run::

  ./whistle.sh --help

You must accept the License terms to run it.

.. |trade|   unicode:: U+2122 .. TRADEMARK
.. _OpenBEL: https://github.com/OpenBEL
.. _Apache Maven: http://maven.apache.org
