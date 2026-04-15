import {
  AngularNodeAppEngine,
  createNodeRequestHandler,
  isMainModule,
  writeResponseToNodeResponse,
} from '@angular/ssr/node';
import express from 'express';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { request as httpRequest } from 'node:http';
import { URL } from 'node:url';

const serverDistFolder = dirname(fileURLToPath(import.meta.url));
const browserDistFolder = resolve(serverDistFolder, '../browser');

const app = express();
const angularApp = new AngularNodeAppEngine();

// ── Backend API Proxy (Node.js native) ─────────────────────
const BACKEND_URL = process.env['BACKEND_URL'] || 'http://backend:3200';

const PROXY_PATHS = ['/api', '/ws', '/register', '/login', '/profil', '/updateProfile', '/changePassword', '/deleteAccount'];

function proxyToBackend(req: any, res: any) {
  const target = new URL(BACKEND_URL);
  const options = {
    hostname: target.hostname,
    port: target.port,
    path: req.originalUrl,
    method: req.method,
    headers: { ...req.headers, host: target.host },
  };

  const proxyReq = httpRequest(options, (proxyRes) => {
    res.writeHead(proxyRes.statusCode || 500, proxyRes.headers);
    proxyRes.pipe(res, { end: true });
  });

  proxyReq.on('error', (err) => {
    console.error('Proxy error:', err.message);
    if (!res.headersSent) {
      res.status(502).json({ error: 'Backend unavailable' });
    }
  });

  req.pipe(proxyReq, { end: true });
}

PROXY_PATHS.forEach((path) => {
  app.get(path, proxyToBackend);
  app.get(`${path}/**`, proxyToBackend);
  app.post(path, proxyToBackend);
  app.post(`${path}/**`, proxyToBackend);
  app.put(path, proxyToBackend);
  app.put(`${path}/**`, proxyToBackend);
  app.delete(path, proxyToBackend);
  app.delete(`${path}/**`, proxyToBackend);
  app.patch(path, proxyToBackend);
  app.patch(`${path}/**`, proxyToBackend);
});

/**
 * Serve static files from /browser
 */
app.use(
  express.static(browserDistFolder, {
    maxAge: '1y',
    index: false,
    redirect: false,
  }),
);

/**
 * Handle all other requests by rendering the Angular application.
 */
app.use('/**', (req, res, next) => {
  angularApp
    .handle(req)
    .then((response) =>
      response ? writeResponseToNodeResponse(response, res) : next(),
    )
    .catch(next);
});

/**
 * Start the server if this module is the main entry point.
 * The server listens on the port defined by the `PORT` environment variable, or defaults to 4000.
 */
if (isMainModule(import.meta.url)) {
  const port = process.env['PORT'] || 4000;
  app.listen(port, () => {
    console.log(`Node Express server listening on http://localhost:${port}`);
  });
}

/**
 * Request handler used by the Angular CLI (for dev-server and during build) or Firebase Cloud Functions.
 */
export const reqHandler = createNodeRequestHandler(app);
