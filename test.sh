# test.sh
#
#!/bin/bash

java -jar build/jar/jphantom.jar ../.old/asm/complement/pernasvip-ant/build/jar/itGen.jar --save-class-files
echo 'Differences in output: '
diff -r out/phantoms/org/ ../.old/asm/complement/out/phantoms/org/
