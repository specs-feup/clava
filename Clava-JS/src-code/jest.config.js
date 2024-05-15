const config = {
  preset: "ts-jest/presets/default-esm",
  testEnvironment: "node",
  moduleNameMapper: {
    "(.+)\\.js": "$1",
  },
};

export default config;
