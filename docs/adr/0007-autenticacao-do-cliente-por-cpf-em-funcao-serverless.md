# Autenticação do cliente por CPF em função serverless, com o JWT que a API já valida

O cliente passa a ter um canal autenticado: informa o CPF em `POST /v1/auth/customer`, no API
Gateway, e recebe um JWT com o qual consome `/v1/me/work-orders` — listar e detalhar as próprias
ordens e aprovar ou recusar o orçamento delas. Quem emite o token é uma Lambda (`lambda/customer-auth`),
não a API: ela valida o CPF, consulta no RDS se o cliente existe e está ativo, e assina o token.

A Lambda assina com a mesma chave privada RS256 da API, com o mesmo issuer, o id do cliente em `sub`
e o papel `CUSTOMER` em `groups`. Para a API o token é indistinguível de um que ela mesma tivesse
emitido, e por isso nada mudou na validação: o SmallRye JWT confere assinatura, issuer e validade, e o
RBAC por `@RolesAllowed` separa o cliente das rotas administrativas. A alternativa — um authorizer no
API Gateway validando um token próprio da função — duplicaria a regra de autorização em dois lugares e
deixaria a API exposta a quem alcançasse o ELB por fora do gateway.

O preço é que a chave privada passa a existir em dois lugares, o Secret do pod e a variável de
ambiente da função, e rotacioná-la exige atualizar os dois. A variável é criptografada em repouso pela
Lambda e o valor vive no state do Terraform, que está no S3 criptografado. Secrets Manager seria o
lugar natural, mas a função roda dentro da VPC para alcançar o RDS (`publicly_accessible = false`), e
a VPC default não tem NAT: buscar o segredo exigiria um VPC endpoint de interface, custo fixo que o
ambiente de avaliação não justifica.

A função lê a tabela `customer` diretamente, em vez de chamar a API. É o que o enunciado pede
("consultar a existência e o status do cliente na base de dados"), e mantém o login de pé mesmo com o
cluster fora do ar. O acoplamento é com três colunas — `customer_id`, `document`/`document_type` e
`active` — cobertas pelo teste de integração da função contra a mesma estrutura que a migration V3
deixa.

Cliente inexistente e cliente inativo recebem a mesma resposta, `401 CUSTOMER_NOT_AUTHORIZED`: dizer
qual dos dois ocorreu permitiria descobrir, testando CPFs, quem é cliente da oficina. O motivo real
fica só no log, em JSON e sem o CPF. Pela mesma razão, a ordem de outro cliente responde `404` nas
rotas `/v1/me`, e não `403`.

O token do cliente vale 60 minutos, contra 8 horas do funcionário: o CPF não é segredo, então a sessão
que ele abre é curta, e desativar o cliente (`PATCH /v1/customer/{id}/deactivate`) corta novos logins
na hora — os tokens já emitidos expiram em até uma hora.
