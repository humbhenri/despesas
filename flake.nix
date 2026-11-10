
{
  description = "Ambiente de desenvolvimento Clojure";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
    nixpkgs.inputs.nixpkgs.follows = "nixpkgs";
    devDB.url = "github:hermann-p/nix-postgres-dev-db";
    devDB.inputs.nixpkgs.follows = "nixpkgs";
  };

  outputs = { self, nixpkgs, devDB }:
    let
      system = "x86_64-linux";
      pkgs = import nixpkgs {
        inherit system;
      };
      db = devDB.outputs.packages.${system};
    in
    {
      devShells.${system}.default = pkgs.mkShell {
        buildInputs = [
              pkgs.postgresql_15
              db.start-database
              db.stop-database
              db.psql-wrapped
            ];
        packages = with pkgs; [
          nodejs_24
          pkgs.typescript
          pkgs.typescript-language-server
 	        tree-sitter
          clojure
        ];

        shellHook = ''
          echo "Ambiente carregado"
          clojure --version
          npm --version
          export PG_ROOT=$(git rev-parse --show-toplevel)
        '';
      };
    };
}

