[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [ValidatePattern('^M-[A-Z0-9-]{8,29}$')]
    [string]$MerchantId,
    [ValidateSet('Issue', 'Disable')]
    [string]$Action = 'Issue',
    [string]$Database = 'huizhipay_local',
    [string]$HostName = '127.0.0.1',
    [ValidateRange(1, 65535)] [int]$Port = 15432,
    [string]$Username = 'huizhipay',
    [string]$PsqlPath = 'psql'
)

$ErrorActionPreference = 'Stop'
function SqlLiteral([string]$Value) { return "'" + $Value.Replace("'", "''") + "'" }
$merchantSql = SqlLiteral $MerchantId

if ($Action -eq 'Disable') {
    $sql = "update t_merchant_api_key set enabled=false, disabled_at=(now() at time zone 'utc') where merchant_id=$merchantSql and enabled=true returning key_prefix;"
    $result = & $PsqlPath -X -v ON_ERROR_STOP=1 -h $HostName -p $Port -U $Username -d $Database -c $sql 2>&1
    if ($LASTEXITCODE -ne 0) { throw ($result -join [Environment]::NewLine) }
    Write-Output "Test API key disabled for merchant=$MerchantId"
    return
}

$bytes = New-Object byte[] 36
[Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
$raw = 'hzp_test_' + [Convert]::ToBase64String($bytes).TrimEnd('=').Replace('+','-').Replace('/','_')
$hashBytes = [Security.Cryptography.SHA256]::HashData([Text.Encoding]::UTF8.GetBytes($raw))
$hash = [Convert]::ToHexString($hashBytes).ToLowerInvariant()
$prefix = $raw.Substring(0, [Math]::Min(20, $raw.Length))
$hashSql = SqlLiteral $hash
$prefixSql = SqlLiteral $prefix
$sql = @"
begin;
do `$check`$
begin
  if not exists (select 1 from t_merchant where merchant_id=$merchantSql and kyb_status='APPROVED') then
    raise exception 'API key issuance requires exactly one approved test merchant';
  end if;
end
`$check`$;
update t_merchant_api_key set enabled=false, disabled_at=(now() at time zone 'utc') where merchant_id=$merchantSql and enabled=true;
insert into t_merchant_api_key(merchant_id, key_prefix, key_hash) values ($merchantSql, $prefixSql, $hashSql);
commit;
"@
$result = & $PsqlPath -X -v ON_ERROR_STOP=1 -h $HostName -p $Port -U $Username -d $Database -c $sql 2>&1
if ($LASTEXITCODE -ne 0) { throw ($result -join [Environment]::NewLine) }
Write-Output "Test API key issued for merchant=$MerchantId"
Write-Output "Copy this key now; it is shown once and only its SHA-256 digest is stored:"
Write-Output $raw
