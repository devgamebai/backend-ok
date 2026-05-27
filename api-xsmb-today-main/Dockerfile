FROM node:20.14-alpine
ADD package.json package.json
RUN npm install
ADD . .

ENTRYPOINT [ "node", "app.js" ]