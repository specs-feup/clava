import { jest } from "@jest/globals";

jest.mock("java", () => global.__SHARED_MODULE__);
