[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [ValidatePattern('^M-[A-Z0-9-]{8,29}$')]
    [string]$MerchantId,

    [Parameter(Mandatory = $true)]
    [ValidatePattern('^[^@\s]+@[^@\s]+\.[^@\s]+$')]
    [string]$OwnerEmail,

    [Parameter(Mandatory = $true)]
    [ValidateLength(2, 128)]
    [string]$Reviewer,

    [string]$Database = 'huizhipay_sandbox',
    [string]$HostName = '127.0.0.1',
    [ValidateRange(1, 65535)]
    [int]$Port = 15432,
    [string]$Username = 'huizhipay',
    [string]$PsqlPath = 'psql'
)

$ErrorActionPreference = 'Stop'

if ($Database -notmatch '(^|_)sandbox($|_)') {
    throw "Refusing KYB approval: database name '$Database' is not an explicit sandbox database."
}
if ($HostName -notin @('127.0.0.1', 'localhost')) {
    throw "Refusing KYB approval: sandbox review is restricted to loopback PostgreSQL."
}

function ConvertTo-SqlLiteral([string]$Value) {
    return "'" + $Value.Replace("'", "''") + "'"
}

$merchantSql = ConvertTo-SqlLiteral $MerchantId
$emailSql = ConvertTo-SqlLiteral $OwnerEmail
$reviewerSql = ConvertTo-SqlLiteral $Reviewer

$sql = @"
begin;

create temporary table approved_kyb on commit drop as
with candidate as (
  select m.id, m.merchant_id, u.email, m.kyb_status
  from t_merchant m
  join t_user u on u.id = m.owner_user_id
  where m.merchant_id = $merchantSql
    and lower(u.email) = lower($emailSql)
    and m.kyb_status = 'PENDING'
  for update
), updated as (
  update t_merchant m
  set kyb_status = 'APPROVED',
      reviewed_at = (now() at time zone 'utc'),
      updated_at = (now() at time zone 'utc')
  from candidate c
  where m.id = c.id
  returning m.merchant_id, c.email, c.kyb_status
)
select * from updated;

do `$validation`$
begin
  if (select count(*) from approved_kyb) <> 1 then
    raise exception 'Approval requires exactly one matching PENDING merchant and owner email';
  end if;
end
`$validation`$;

insert into t_kyb_review_audit
  (merchant_id, owner_email, reviewer, previous_status, decision, source, reviewed_at)
select merchant_id, email, $reviewerSql, kyb_status, 'APPROVED', 'SANDBOX_SCRIPT',
       (now() at time zone 'utc')
from approved_kyb;

commit;
"@

$result = & $PsqlPath -X -v ON_ERROR_STOP=1 -h $HostName -p $Port -U $Username -d $Database -c $sql 2>&1
if ($LASTEXITCODE -ne 0) {
    throw ($result -join [Environment]::NewLine)
}

Write-Output "KYB approved in sandbox: merchant=$MerchantId owner=$OwnerEmail reviewer=$Reviewer"
