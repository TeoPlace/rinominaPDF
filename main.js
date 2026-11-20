const pkg = require('./package.json');

require('@teoplace/kit').entry(
  {
    NAME: 'rinominaPDF',
    EXE: 'rinominaPDF.exe',
    VERSION: pkg.version,
    HOME_URL: 'https://rinominapdf.teo.place/',
    CLAUDE_URL: 'https://rinominapdf.teo.place/claude',
    MCP_SERVER_KEY: 'rinominapdf',
    MCP_PORT: 0,
    UPGRADE_CODE: '6E50594E-22A6-3AC3-A7FB-48CFCA119568'
  },
  () => require('./build/main/app.js')
);
