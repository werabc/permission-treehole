import http from "node:http";

const LISTEN_PORT = 3081;
const TARGET_HOST = "127.0.0.1";
const TARGET_PORT = 3080;

const server = http.createServer((req, res) => {
  const options = {
    hostname: TARGET_HOST,
    port: TARGET_PORT,
    path: req.url,
    method: req.method,
    headers: { ...req.headers, host: `${TARGET_HOST}:${TARGET_PORT}` },
  };
  const proxyReq = http.request(options, (proxyRes) => {
    res.writeHead(proxyRes.statusCode, proxyRes.headers);
    proxyRes.pipe(res);
  });
  proxyReq.on("error", (err) => {
    console.error("Proxy error:", err.message);
    res.writeHead(502);
    res.end("Bad Gateway");
  });
  req.pipe(proxyReq);
});

server.on("upgrade", (req, socket, head) => {
  const { EventEmitter } = require("events");
  const net = require("net");
  const proxyConn = net.createConnection(TARGET_PORT, TARGET_HOST);
  proxyConn.on("connect", () => {
    proxyConn.write(
      `${req.method} ${req.url} HTTP/${req.httpVersion}\r\n`
    );
    for (let i = 0; i < req.rawHeaders.length; i += 2) {
      proxyConn.write(`${req.rawHeaders[i]}: ${req.rawHeaders[i + 1]}\r\n`);
    }
    proxyConn.write("\r\n");
    if (head.length) proxyConn.write(head);
    socket.pipe(proxyConn).pipe(socket);
  });
  proxyConn.on("error", () => socket.destroy());
  socket.on("error", () => proxyConn.destroy());
});

server.listen(LISTEN_PORT, "0.0.0.0", () => {
  console.log(`DSH LAN proxy running: http://0.0.0.0:${LISTEN_PORT} -> http://${TARGET_HOST}:${TARGET_PORT}`);
  console.log(`Phone: http://192.168.31.106:${LISTEN_PORT}`);
});

server.on("request", (req) => {
  console.log(`[${new Date().toISOString()}] ${req.method} ${req.url}`);
});
