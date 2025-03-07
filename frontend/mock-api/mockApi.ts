import Chance from "chance";
import { Application } from "express";
import path from "path";
import { fileURLToPath } from "url";
import packagejson from "../package.json" assert { type: "json" };
import config from "./config.json" assert { type: "json" };
import EXPORT_FORM_CONFIG from "./forms/export-form.json" assert { type: "json" };
import mockAuthMiddleware from "./mockAuthMiddleware.js";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const version = packagejson.version;

config.version = version;

const FRONTEND_CONFIG = config;

const chance = new Chance();

// Taken from:
// http://stackoverflow.com/questions/2450954/how-to-randomize-shuffle-a-javascript-array
function shuffleArray<T>(array: T[]) {
  for (var i = array.length - 1; i > 0; i--) {
    var j = Math.floor(Math.random() * (i + 1));
    var temp = array[i];
    array[i] = array[j];
    array[j] = temp;
  }
  return array;
}

const ERROR = JSON.stringify({
  message: "Could not process the request",
});

const LONG_DELAY = 500;
const SHORT_DELAY = 300;
const NO_DELAY = 10;

// Simulate API
export default function mockApi(app: Application) {
  /*
    QUERIES
  */
  app.post(
    "/api/datasets/:datasetId/queries",
    mockAuthMiddleware,
    function response(_, res) {
      const dice = Math.random();

      setTimeout(() => {
        if (dice < 0.3) {
          res.status(422);
          res.send(ERROR);
        } else {
          res.setHeader("Content-Type", "application/json");
          res.status(201);
          res.send(JSON.stringify({ id: 1 }));
        }
      }, NO_DELAY);
    },
  );

  app.post(
    "/api/queries/:id/cancel",
    mockAuthMiddleware,
    function response(_, res) {
      setTimeout(() => {
        res.setHeader("Content-Type", "application/json");
        res.send(JSON.stringify({ id: 1 }));
      }, SHORT_DELAY);
    },
  );

  app.post(
    "/api/datasets/:datasetId/queries/upload",
    mockAuthMiddleware,
    function response(_, res) {
      const unresolvedId: string[][] = [];
      for (let i = 0; i < 3000; i++) {
        unresolvedId.push(chance.sentence({ words: 3 }).split(" "));
      }

      const successResponse = JSON.stringify({
        id: 234,
        resolved: Math.floor(Math.random() * 2000),
        unresolvedId: [],
        unreadableDate: [],
      });
      const errorResponse = JSON.stringify({
        id: 234,
        resolved: Math.floor(Math.random() * 2),
        unresolvedId,
        unreadableDate: [
          ["xyz", "yes", "hello"],
          ["abc", "no", "good day"],
          ["def", "yes", "tomorrow"],
        ],
      });

      setTimeout(() => {
        res.setHeader("Content-Type", "application/json");
        if (Math.random() < 0.2) {
          res.send(successResponse);
        } else {
          res.send(errorResponse);
        }
      }, SHORT_DELAY);
    },
  );

  app.delete("/api/queries/:id", mockAuthMiddleware, function response(_, res) {
    setTimeout(() => {
      res.setHeader("Content-Type", "application/json");
      res.send(JSON.stringify({ id: 1 }));
    }, SHORT_DELAY);
  });

  app.get("/api/queries/:id", mockAuthMiddleware, function response(req, res) {
    if (req.params.id !== "1") {
      setTimeout(() => {
        res.sendFile(path.join(__dirname, "./stored-queries/25.json"));
      }, LONG_DELAY);
    } else {
      setTimeout(() => {
        res.setHeader("Content-Type", "application/json");

        const dice = Math.random();

        if (dice <= 0.1) {
          res.status(422);
          res.send(ERROR);
        } else if (dice > 0.1 && dice <= 0.7) {
          res.send(
            JSON.stringify({
              id: 1,
              status: "RUNNING",
              progress: Math.floor(Math.random() * 10) / 10,
            }),
          );
        } else if (dice > 0.7 && dice <= 0.75) {
          res.send(
            JSON.stringify({
              id: 1,
              status: "FAILED",
              error: {
                message: "This is an example message",
              },
            }),
          );
        } else {
          res.send(
            JSON.stringify({
              id: 1,
              status: "DONE",
              label: "Test result",
              numberOfResults: 5,
              resultUrls:
                dice > 0.85
                  ? [
                      {
                        label: "XLSX",
                        url: "/api/results/results.xlsx",
                      },
                      {
                        label: "CSV",
                        url: "/api/results/results.csv",
                      },
                    ]
                  : [
                      {
                        label:
                          "Some File with a long label and an exotic file type, which the frontend probably never heard of",
                        url: "http://localhost:8080/api/result/csv/51cd95fd-90b2-4573-aab5-11846126427b.blobby",
                      },
                    ],
              columnDescriptions: [
                {
                  label: "Money Range",
                  selectId: null,
                  type: "MONEY",
                },
              ],
            }),
          );
        }
      }, LONG_DELAY);
    }
  });

  /*
    DATASETS
  */
  app.get("/api/datasets", mockAuthMiddleware, function response(_, res) {
    res.setHeader("Content-Type", "application/json");

    res.send(
      JSON.stringify([
        { id: "imdb", label: "IMDb" },
        { id: "empty-set", label: "Empty Dataset" },
        {
          id: "another-empty-set",
          label: "Another empty dataset with a long name",
        },
      ]),
    );
  });

  /*
    QUERY RESULT DOWNLOAD
  */
  app.get(
    "/api/results/:filename",
    mockAuthMiddleware,
    function response(req, res) {
      res.sendFile(path.join(__dirname, `./results/${req.params.filename}`));
    },
  );

  /*
    CONCEPTS
  */
  app.get(
    "/api/datasets/:id/concepts",
    mockAuthMiddleware,
    function response(_, res) {
      res.sendFile(path.join(__dirname, "./concepts.json"));
    },
  );

  app.get("/api/concepts/:id", mockAuthMiddleware, function response(req, res) {
    res.sendFile(path.join(__dirname, `./concepts/${req.params.id}.json`));
  });

  /*
    STORED QUERIES
  */
  app.get(
    "/api/datasets/:datasetId/queries",
    mockAuthMiddleware,
    function response(_, res) {
      res.setHeader("Content-Type", "application/json");

      setTimeout(() => {
        const ids: unknown[] = [];
        const possibleTagsWithProbabilities = [
          ["research", 0.3],
          ["fun", 0.02],
          ["test", 0.02],
          ["group 1", 0.2],
          ["group 1 – details", 0.2],
          ["important", 0.02],
          ["jk", 0.02],
          ["interesting", 0.03],
          ["a rather long tagname", 0.001],
          ["Another very long long tagname, 2020", 0.001],
        ];

        for (var i = 24700; i < 25700; i++) {
          const notExecuted = Math.random() < 0.1;

          ids.push({
            id: i,
            label: Math.random() > 0.1 ? chance.sentence({ words: 8 }) : null,
            numberOfResults: notExecuted
              ? null
              : Math.floor(Math.random() * 500000),
            tags: shuffleArray(
              possibleTagsWithProbabilities
                .filter(([, prob]) => Math.random() < prob)
                .map(([tag]) => tag),
            ),
            createdAt: new Date(
              Date.now() - Math.floor(Math.random() * 10000000),
            ).toISOString(),
            own: Math.random() < 0.1,
            canExpand: Math.random() < 0.8,
            shared: Math.random() < 0.8,
            resultUrls: notExecuted
              ? []
              : [
                  {
                    label: "XLSX",
                    url: "http://localhost:8080/api/result/xlsx/51cd95fd-90b2-4573-aab5-11846126427b.xlsx",
                  },
                  {
                    label: "CSV",
                    url: "http://localhost:8080/api/result/csv/51cd95fd-90b2-4573-aab5-11846126427b.csv",
                  },
                  {
                    label: "Some File with a long name and an exotic file type",
                    url: "http://localhost:8080/api/result/csv/51cd95fd-90b2-4573-aab5-11846126427b.blobby",
                  },
                ],

            ownerName: "System",
            ...(Math.random() > 0.2
              ? { queryType: "CONCEPT_QUERY" }
              : {
                  queryType: "SECONDARY_ID_QUERY",
                  secondaryId: "fun_fall_id",
                }),
          });
        }

        res.send(JSON.stringify(ids));
      }, LONG_DELAY);
    },
  );

  app.patch("/api/queries/:id", mockAuthMiddleware, function response(_, res) {
    setTimeout(() => {
      res.send(JSON.stringify({}));
    }, LONG_DELAY);
  });

  app.delete("/api/queries/:id", mockAuthMiddleware, function response(_, res) {
    setTimeout(() => {
      res.setHeader("Content-Type", "application/json");
      res.send(JSON.stringify({ id: 1 }));
    }, SHORT_DELAY);
  });

  app.get(
    "/api/datasets/:datasetId/form-queries",
    mockAuthMiddleware,
    function response(_, res) {
      setTimeout(() => {
        res.setHeader("Content-Type", "application/json");
        res.send(JSON.stringify(EXPORT_FORM_CONFIG));
      }, SHORT_DELAY);
    },
  );

  app.post(
    "/api/datasets/:datasetId/import",
    mockAuthMiddleware,
    function response(_, res) {
      setTimeout(() => {
        res.setHeader("Content-Type", "application/json");
        res.status(201);
        res.send(
          JSON.stringify({
            successful: 1 + Math.floor(Math.random() * 200),
            unsuccessful: 586,
          }),
        );
      }, LONG_DELAY);
    },
  );

  app.post(
    "/api/filters/:filterId/autocomplete",
    mockAuthMiddleware,
    function response(req, res) {
      setTimeout(() => {
        res.setHeader("Content-Type", "application/json");

        const text = req.body.text.toLowerCase();
        const countriesRequested = req.params.filterId === "production_country";
        const wordsRequested = req.params.filterId === "words";

        const storedValues: string[] = countriesRequested
          ? require("./autocomplete/countries")
          : wordsRequested
          ? require("./autocomplete/words")
          : [
              "1008508208",
              "1015841172",
              "1011218302",
              "1007680436",
              "1017776144",
              "1003780588",
              "1000326535",
              "1014150881",
              "1017126347",
              "1008445564",
            ];

        const suggestions = storedValues
          .map((v, id) => ({
            label: v,
            value: id,
            templateValues: { company: "Columbia Pictures Corporation" },
          }))
          .filter((v) => v.label.toLowerCase().startsWith(text));

        res.send(JSON.stringify(suggestions));
      }, LONG_DELAY);
    },
  );

  app.post(
    "/api/concepts/:conceptId/resolve",
    mockAuthMiddleware,
    function response(req, res) {
      setTimeout(() => {
        res.setHeader("Content-Type", "application/json");

        const { concepts } = req.body;

        res.send({
          unknownCodes: concepts.slice(5),
          resolvedConcepts: concepts.slice(1),
        });
      }, LONG_DELAY);
    },
  );

  /*
    VERSION
  */
  app.get("/api/version", function (_, res) {
    res.setHeader("Content-Type", "application/json");

    res.send({
      version: version,
      isDevelopment: process.env.NODE_ENV !== "production",
    });
  });

  /*
    For DND File see ./app/api/dnd
  */
  app.post(
    "/api/filters/:filterId/resolve",
    mockAuthMiddleware,
    function response(req, res) {
      setTimeout(() => {
        res.setHeader("Content-Type", "application/json");

        const { values } = req.body;

        if (req.params.filterId !== "production_country") return null;

        const countries = require("./autocomplete/countries");
        const unknownCodes = (values as string[]).filter(
          (val) => !countries.includes(val),
        );
        const resolvedValues = (values as string[]).filter((val) =>
          countries.includes(val),
        );

        res.send({
          unknownCodes: unknownCodes,
          resolvedFilter: {
            tableId: "some-tableId",
            filterId: req.params.filterId,
            value: resolvedValues.map((val) => ({ label: val, value: val })),
          },
        });
      }, LONG_DELAY);
    },
  );

  app.get("/api/config/frontend", mockAuthMiddleware, (_, res) => {
    res.setHeader("Content-Type", "application/json");

    res.send(FRONTEND_CONFIG);
  });

  app.post("/auth", function response(req, res) {
    setTimeout(() => {
      res.setHeader("Content-Type", "application/json");

      const { user, password } = req.body;

      if (user === "test" && password === "test") {
        res.send({
          access_token: "VALID",
        });
      } else {
        res.status(422);
        res.send(
          JSON.stringify({
            message: "Login failed",
          }),
        );
      }
    }, 500);
  });

  app.get("/api/me", mockAuthMiddleware, (_, res) => {
    res.setHeader("Content-Type", "application/json");

    res.send({
      userName: "superUser",
      datasetAbilities: {
        imdb: {
          canUpload: true,
          canViewEntityPreview: true,
          canViewQueryPreview: true,
        },
      },
      groups: [],
      hideLogoutButton: false,
    });
  });

  app.post(
    "/api/datasets/:datasetId/form-configs",
    mockAuthMiddleware,
    function response(_, res) {
      setTimeout(() => {
        res.setHeader("Content-Type", "application/json");
        res.status(201);
        res.send(
          JSON.stringify({
            id: 56000 + Math.floor(Math.random() * 200),
          }),
        );
      }, LONG_DELAY);
    },
  );

  app.get(
    "/api/datasets/:datasetId/form-configs",
    mockAuthMiddleware,
    function response(_, res) {
      res.setHeader("Content-Type", "application/json");

      function getFormConfigAttributes() {
        const dice = Math.random();

        if (dice < 0.5) {
          return {
            formType: "EXPORT_FORM",
            values: {},
          };
        } else {
          return {
            formType: "Other form",
            values: {},
          };
        }
      }

      setTimeout(() => {
        const configs: unknown[] = [];
        const possibleTags = [
          "research",
          "fun",
          "export",
          "group 1",
          "important",
          "jk",
          "interesting",
        ];

        for (var i = 84600; i < 85600; i++) {
          configs.push({
            id: i,
            label: "Saved Config",
            tags: shuffleArray(possibleTags.filter(() => Math.random() < 0.3)),
            createdAt: new Date(
              Date.now() - Math.floor(Math.random() * 10000000),
            ).toISOString(),
            own: Math.random() < 0.1,
            shared: Math.random() < 0.8,
            ownerName: "System",
            ...getFormConfigAttributes(),
          });
        }

        res.send(JSON.stringify(configs));
      }, LONG_DELAY);
    },
  );

  app.get(
    "/api/form-configs/:id",
    mockAuthMiddleware,
    function response(_, res) {
      setTimeout(() => {
        res.sendFile(path.join(__dirname, "./form-configs/testconf.json"));
      }, LONG_DELAY);
    },
  );

  app.patch(
    "/api/form-configs/:id",
    mockAuthMiddleware,
    function response(_, res) {
      setTimeout(() => {
        res.setHeader("Content-Type", "application/json");
        res.status(200).end();
      }, SHORT_DELAY);
    },
  );

  app.delete(
    "/api/form-configs/:id",
    mockAuthMiddleware,
    function response(_, res) {
      setTimeout(() => {
        res.status(204).end();
      }, LONG_DELAY);
    },
  );
}
