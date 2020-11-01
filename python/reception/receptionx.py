# Test framework to open server port and start recep[tion app.
import socket

import reception

s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
s.bind(('', 0))
addr = s.getsockname()
print(f"Server socket: {addr[1]}")

# Wait for socket connect
s.listen()
(cs, address) = s.accept()

# start actual reception app.
f = cs.makefile(mode='rw', encoding='utf-8', newline='\n')

reception_app = reception.Reception(f, f)
#reception_app = reception.ReceptionML(f, f)
reception_app.start()

cs.close()
s.close()
