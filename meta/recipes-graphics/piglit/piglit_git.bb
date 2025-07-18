SUMMARY = "OpenGL driver testing framework"
DESCRIPTION = "Piglit is an open-source test suite for OpenGL and OpenCL \
implementations."
HOMEPAGE = "https://gitlab.freedesktop.org/mesa/piglit"
BUGTRACKER = "https://gitlab.freedesktop.org/mesa/piglit/-/issues"
LICENSE = "MIT & LGPL-2.0-or-later & GPL-3.0-only & GPL-2.0-or-later & BSD-3-Clause"
LIC_FILES_CHKSUM = "file://COPYING;md5=b2beded7103a3d8a442a2a0391d607b0"

SRC_URI = "git://gitlab.freedesktop.org/mesa/piglit.git;protocol=https;branch=main \
           file://0002-cmake-use-proper-WAYLAND_INCLUDE_DIRS-variable.patch \
           file://0003-tests-util-piglit-shader.c-do-not-hardcode-build-pat.patch \
           file://0001-tests-Fix-narrowing-errors-seen-with-clang.patch \
           file://0001-CMakeLists.txt-do-not-obtain-wayland-scanner-path-fr.patch \
           file://0001-tests-egl-spec-make-egl_ext_surface_compression-cond.patch \
           file://0001-tests-no_error.py-modify-_command-and-not-command.patch \
           "
UPSTREAM_CHECK_COMMITS = "1"

SRCREV = "a0a27e528f643dfeb785350a1213bfff09681950"
# (when PV goes above 1.0 remove the trailing r)
PV = "1.0+gitr"

X11_DEPS = "${@bb.utils.contains('DISTRO_FEATURES', 'x11', 'virtual/libx11 libxrender libglu', '', d)}"
X11_RDEPS = "${@bb.utils.contains('DISTRO_FEATURES', 'x11', 'mesa-demos', '', d)}"

DEPENDS = "libpng waffle libxkbcommon python3-mako-native python3-numpy-native virtual/egl"

inherit cmake pkgconfig python3native features_check bash-completion

# depends on virtual/libgl
REQUIRED_DISTRO_FEATURES += "opengl"

# The built scripts go into the temporary directory according to tempfile
# (typically /tmp) which can race if multiple builds happen on the same machine,
# so tell it to use a directory in ${B} to avoid overwriting.
export TEMP = "${B}/temp/"
do_compile[dirs] =+ "${B}/temp/"

PACKAGECONFIG ??= " \
    ${@bb.utils.contains('DISTRO_FEATURES', 'x11', 'x11 glx', '', d)} \
    ${@bb.utils.contains('DISTRO_FEATURES', 'wayland', 'wayland', '', d)} \
"
PACKAGECONFIG[freeglut] = "-DPIGLIT_USE_GLUT=1,-DPIGLIT_USE_GLUT=0,freeglut,"
PACKAGECONFIG[glx] = "-DPIGLIT_BUILD_GLX_TESTS=ON,-DPIGLIT_BUILD_GLX_TESTS=OFF"
PACKAGECONFIG[opencl] = "-DPIGLIT_BUILD_CL_TESTS=ON,-DPIGLIT_BUILD_CL_TESTS=OFF,virtual/opencl-icd"
PACKAGECONFIG[x11] = "-DPIGLIT_USE_X11=1 -DPIGLIT_BUILD_GL_TESTS=ON -DPIGLIT_BUILD_DMA_BUF_TESTS=ON,-DPIGLIT_USE_X11=0 -DPIGLIT_BUILD_GL_TESTS=OFF -DPIGLIT_BUILD_DMA_BUF_TESTS=OFF,${X11_DEPS}, ${X11_RDEPS}"
PACKAGECONFIG[vulkan] = "-DPIGLIT_BUILD_VK_TESTS=ON,-DPIGLIT_BUILD_VK_TESTS=OFF,glslang-native vulkan-loader,glslang"
PACKAGECONFIG[wayland] = "-DPIGLIT_USE_WAYLAND=1,-DPIGLIT_USE_WAYLAND=0,wayland-native wayland wayland-protocols"

do_configure:prepend() {
   if [ "${@bb.utils.contains('PACKAGECONFIG', 'freeglut', 'yes', 'no', d)}" = "no" ]; then
        sed -i -e "/^#.*include <GL\/freeglut_ext.h>$/d" ${S}/src/piglit/glut_wrap.h
        sed -i -e "/^#.*include.*<GL\/glut.h>$/d" ${S}/src/piglit/glut_wrap.h
   fi
}

# Forcibly strip because Piglit is *huge*, and don't bother trying to split/strip the result.
OECMAKE_TARGET_INSTALL = "install/strip"
INHIBIT_PACKAGE_STRIP = "1"
INHIBIT_PACKAGE_DEBUG_SPLIT = "1"

RDEPENDS:${PN} = "waffle waffle-bin python3 python3-mako python3-json \
	python3-misc \
	python3-unixadmin python3-xml python3-multiprocessing \
        python3-shell python3-io \
	python3-netserver bash \
	"

INSANE_SKIP:${PN} += "dev-so already-stripped"

# As nothing builds against Piglit we don't need to have anything in the
# sysroot, especially when this is ~2GB of test suite
SYSROOT_DIRS:remove = "${libdir}"

# Can't be built with ccache
CCACHE_DISABLE = "1"
