#
# Builds a make target 'package' that zips library and header files. This will
# follow the 'install' structure.
#
# ARGUMENTS:
#   - name : the prefix of the zip file name
#
# EXAMPLE:
#   make_package_target("xpto")
#   creates a target that when invoked (make package) will create a zip file
#   called 'xpto-Linux-gcc4.zip' if we are in Linux with gcc 4.x.y
#

function(make_package_target name)

    # package target
    set(CPACK_GENERATOR "ZIP")
    set(DEPS_PACKAGE_NAME "${name}-${CMAKE_SYSTEM_NAME}-${DEPS_COMPILER}")
    set(DEPS_ZIP_NAME "${CMAKE_BINARY_DIR}/${DEPS_PACKAGE_NAME}.zip")
    set(CPACK_PACKAGE_FILE_NAME ${DEPS_PACKAGE_NAME})
    include(CPack)

    # To create a target for CLion menu and transitive dependency for deploy
    set(DEPS_PHONY_TARGET ${name}-package)
    add_custom_target(${name}-package
        COMMAND $(MAKE) package
        COMMENT "Packaging ${DEPS_PACKAGE_NAME}")

    # deploy target
    add_custom_target(deploy
        COMMAND ${Java_JAVA_EXECUTABLE} -jar ${DEPS_JAR} deploy ${DEPS_ZIP_NAME} ${HOST_SPECS_TOOLS}
        WORKING_DIRECTORY ${DEPS_JAR_DIR}
        COMMENT "Deploying ${DEPS_ZIP_NAME}"
        )
    add_dependencies(deploy ${DEPS_PHONY_TARGET})
    
    # upload target
    add_custom_target(upload
        COMMAND ${Java_JAVA_EXECUTABLE} -jar ${DEPS_JAR} deploy ${DEPS_ZIP_NAME} ${HOST_SPECS_TOOLS}
        WORKING_DIRECTORY ${DEPS_JAR_DIR}
        COMMENT "Uploading ${DEPS_ZIP_NAME}"
        )

endfunction()

