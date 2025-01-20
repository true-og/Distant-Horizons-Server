import sys
import lief

# print("imported", file=sys.stderr)

binary = lief.parse(sys.stdin.buffer.read())
# print([func.name for func in binary.exported_functions], file=sys.stderr)
binary.write(sys.argv[1])
