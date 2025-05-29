# Common issues found when installing or running Clava

## Error 'Invalid or corrupt jarfile'

Check if you have at least Java 17 installed, this is the minimum version.

## Error 'missing libtinfo.so.5'

On Fedora >28 systems this can be solved by installing the package `ncurses-compat-libs`

## Error 'cannot find -lz'

Install libz package (e.g., in Ubuntu `sudo apt-get install libz-dev`)
