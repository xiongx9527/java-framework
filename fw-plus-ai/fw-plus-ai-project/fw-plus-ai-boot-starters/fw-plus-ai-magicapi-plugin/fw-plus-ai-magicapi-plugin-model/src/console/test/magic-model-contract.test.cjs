const fs = require('fs');
const path = require('path');
const test = require('node:test');
const assert = require('node:assert');

const root = path.resolve(__dirname, '..');

test('uses model datasource resource type and plugin name', () => {
  const files = [
    path.join(root, 'src/service/magic-model-datasource.js'),
    path.join(root, 'src/index.js'),
    path.join(root, 'src/components/magic-model-datasource-info.vue')
  ].map(file => fs.readFileSync(file, 'utf8')).join('\n');

  assert.match(files, /model-datasource/);
  assert.match(files, /MagicModel|magic-model/);
  assert.doesNotMatch(files, new RegExp('langchain4j' + '-datasource'));
  assert.doesNotMatch(files, new RegExp('Magic' + 'Langchain4j'));
});
