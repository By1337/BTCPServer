if [ ! "$CC" ]; then
  export CC=gcc
fi

cd "c" || exit

if [ ! -d libdeflate ]; then
  echo "Cloning libdeflate..."
  git clone --branch v1.21 --single-branch https://github.com/ebiggers/libdeflate.git
fi

ARCH=$(uname -m)
suffix=""
if ldd --version 2>&1 | grep -q musl; then
  suffix="-musl"
fi
cmake -DCMAKE_POSITION_INDEPENDENT_CODE=ON -B build && cmake --build build
cd ..
mkdir -p result/linux_$ARCH
mv /app/c/build/libLibdeflateJNI.so /app/result/linux_$ARCH/btcp-libdeflate$suffix.so