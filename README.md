# Despesas - gerenciamento de despesas financeiras da casa

## Desenvolvimento

No emacs, M-x cider-jack-in abre o REPL de desenvolvimento. No REPL,
`(component-repl/reset)` inicia/reinicia o servidor web em
http://localhost:8080.

### Testes

Executar `clj -X:test` no root do projeto ou usar o script test.sh.

### Build

``` shell
clojure -T:build uber
```

ou o script build.sh.
