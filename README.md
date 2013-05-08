JPhantom
========

A tool for Java program complementation. It takes a single jar as its
main argument and complements it by creating a new jar that adds dummy
implementations for every phantom class detected in the original
one. (A *phantom class* is a class that is referenced somewhere but
its definition is missing.)

The phantom classes in the produced jar will contain every missing
field and method that was referenced and used in the original jar, as
well as a supertype that respects every type constraint that was found
(e.g., if phantom class B was used in a place where known class A was
expected, and a [widening reference
conversion](http://docs.oracle.com/javase/specs/jls/se7/html/jls-5.html#jls-5.1.5)
took place, then we must conclude that class A is a supertype of B).

Usage
-----

    java -jar <jphantom>  <injar> [--debug] [--help] [--save-class-files] [-d <dir>] [-o <outjar>] [-v (--log, --verbose) N]
    
     <injar>                 : the jar to be complemented
     --debug                 : Debug mode
     --help                  : Help
     --save-class-files      : Save phantom class files
     -d <dir>                : Phantom-classes destination directory
     -o <outjar>             : the destination path of the complemented jar
     -v (--log, --verbose) N : Level of verbosity
