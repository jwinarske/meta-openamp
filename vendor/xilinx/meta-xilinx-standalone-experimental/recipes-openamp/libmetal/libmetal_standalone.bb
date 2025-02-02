require ${LAYER_PATH_openamp-layer}/recipes-openamp/libmetal/libmetal.inc

SRCREV = "7e6ac3f659724204fd5917952fafb74478c39e43"
S = "${WORKDIR}/git"
B = "${WORKDIR}/build"

SRC_URI:armv7r:xilinx-standalone = "git://gitenterprise.xilinx.com/OpenAMP/libmetal.git;branch=xlnx_decoupling"

OECMAKE_SOURCEPATH = "${S}/"
PROVIDES:armv7r:xilinx-standalone = "libmetal "
DEPENDS:append:armv7r:xilinx-standalone = " libxil scugic doxygen-native xilstandalone"
inherit cmake
LICENSE = "BSD"
LIC_FILES_CHKSUM = "file://LICENSE.md;md5=1ff609e96fc79b87da48a837cbe5db33"

XLNX_STNDALONE_DO_CONFIGURE = ""
XLNX_STNDALONE_DO_CONFIGURE:armv7r:xilinx-standalone = "device-tree-lops:do_deploy"
do_configure[depends] += "${XLNX_STNDALONE_DO_CONFIGURE}"

EXTRA_OECMAKE:armv7r:xilinx-standalone = " \
	-DLIB_INSTALL_DIR=${libdir} \
	-DSOC_FAMILY="${SOC_FAMILY}" \
	-DWITH_EXAMPLES=ON \
	-DWITH_DOCS=OFF \
"

ALLOW_EMPTY:${PN}-demos = "1"

FILES:${PN}-demos:armv7r:xilinx-standalone = " \
    ${bindir}/libmetal_* \
    ${bindir}/*ocm_demo.elf \
"

COMPATIBLE_HOST = ".*-elf"
COMPATIBLE_HOST:arm = "[^-]*-[^-]*-eabi"

LIBMETAL_CMAKE_MACHINE:versal = "Versal"
LIBMETAL_CMAKE_MACHINE:zynqmp = "Zynqmp"

def get_cross_prefix(oe_cmake_c_compiler):
  if oe_cmake_c_compiler == 'arm-xilinx-eabi-gcc':
    return 'arm-xilinx-eabi-'

LIBMETAL_CROSS_PREFIX:armv7r:xilinx-standalone = "${@get_cross_prefix(d.getVar('OECMAKE_C_COMPILER'))}"

def get_libmetal_machine(soc_family):
  if soc_family in ['versal']:
    return 'zynqmp_r5'
  return ''


LIBMETAL_MACHINE:armv7r:xilinx-standalone = "${@get_libmetal_machine(d.getVar('SOC_FAMILY'))}"

cmake_do_generate_toolchain_file:armv7r:xilinx-standalone:append() {
    cat >> ${WORKDIR}/toolchain.cmake <<EOF
    set( CMAKE_SYSTEM_PROCESSOR "${TRANSLATED_TARGET_ARCH}" )
    set( MACHINE "${LIBMETAL_MACHINE}" )
    set( CMAKE_MACHINE "${LIBMETAL_CMAKE_MACHINE}" )
    SET(CMAKE_EXE_LINKER_FLAGS "${CMAKE_EXE_LINKER_FLAGS} ")
    SET(CMAKE_C_ARCHIVE_CREATE "<CMAKE_AR> qcs <TARGET> <LINK_FLAGS> <OBJECTS>")
    set( CMAKE_SYSTEM_NAME "Generic")
    set (CMAKE_FIND_ROOT_PATH_MODE_PROGRAM NEVER CACHE STRING "")
    set (CMAKE_FIND_ROOT_PATH_MODE_LIBRARY NEVER CACHE STRING "")
    set (CMAKE_FIND_ROOT_PATH_MODE_INCLUDE NEVER CACHE STRING "")

    include (CMakeForceCompiler)
    CMAKE_FORCE_C_COMPILER("${OECMAKE_C_COMPILER}" GNU)
    set (CROSS_PREFIX           "${LIBMETAL_CROSS_PREFIX}" CACHE STRING "")
    set (CMAKE_LIBRARY_PATH "${S}/../recipe-sysroot/usr/lib" CACHE STRING "")
    SET(CMAKE_C_ARCHIVE_FINISH   true)
    set (CMAKE_INCLUDE_PATH "${S}/../recipe-sysroot/usr/include/" CACHE STRING "")
    include (cross-generic-gcc)
    add_definitions(-DWITH_DOC=OFF)
EOF
}

# deploy for other recipes
DEPLOY_MACHINE = "${@ d.getVar('MACHINE_ARCH').replace('_','-') }"
SHOULD_DEPLOY = "${@'true' if ( 'Standalone' in  d.getVar('DISTRO_NAME') ) else 'false'}"
do_deploy() {
    echo "get the following: ";
    if ${SHOULD_DEPLOY}; then
        install -Dm 0644 ${D}/usr/bin/*.elf ${DEPLOY_DIR}/images/${DEPLOY_MACHINE}/
    fi
}
addtask deploy before do_build after do_install
