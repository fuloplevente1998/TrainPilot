"""Print the SHA-256 of the first certificate in an APK v2 signing block."""
import hashlib
import struct
import sys
from pathlib import Path


def u32(data, offset):
    return struct.unpack_from('<I', data, offset)[0]


def u64(data, offset):
    return struct.unpack_from('<Q', data, offset)[0]


def length_prefixed(data, offset):
    length = u32(data, offset)
    end = offset + 4 + length
    if end > len(data):
        raise ValueError('Truncated APK signing block')
    return data[offset + 4:end], end


def certificate_sha256(path):
    data = Path(path).read_bytes()
    end_of_zip = data.rfind(b'PK\x05\x06')
    if end_of_zip < 0:
        raise ValueError('ZIP end record missing')
    central_directory = u32(data, end_of_zip + 16)
    if data[central_directory - 16:central_directory] != b'APK Sig Block 42':
        raise ValueError('APK v2 signing block missing')
    size = u64(data, central_directory - 24)
    position = central_directory - size - 8
    if position < 0 or u64(data, position) != size:
        raise ValueError('Invalid APK signing block size')
    position += 8
    while position < central_directory - 24:
        pair_size = u64(data, position)
        pair_id = u32(data, position + 8)
        if pair_size < 4 or position + 8 + pair_size > central_directory - 24:
            raise ValueError('Invalid APK signing pair')
        if pair_id == 0x7109871A:
            signers, _ = length_prefixed(data[position + 12:position + 8 + pair_size], 0)
            signer, _ = length_prefixed(signers, 0)
            signed_data, _ = length_prefixed(signer, 0)
            _, offset = length_prefixed(signed_data, 0)  # digests
            certificates, _ = length_prefixed(signed_data, offset)
            certificate, _ = length_prefixed(certificates, 0)
            return hashlib.sha256(certificate).hexdigest()
        position += 8 + pair_size
    raise ValueError('APK v2 signer missing')


if __name__ == '__main__':
    print(certificate_sha256(sys.argv[1]))
