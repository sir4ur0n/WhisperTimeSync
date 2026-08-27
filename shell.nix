let
  srcs = import nix/sources.nix {};
  pkgs = import srcs.nixpkgs {};
in

pkgs.mkShellNoCC {
  packages = with pkgs; [
    niv
    openjdk21
  ];
}