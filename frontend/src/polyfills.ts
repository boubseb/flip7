// Polyfills for SockJS and older libraries
(window as any).global = window;
(window as any).process = {
  env: { DEBUG: undefined },
};
(window as any).Buffer = (window as any).Buffer || {
  isBuffer: () => false
};
