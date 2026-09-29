use bytesize::ByteSize;
use k8s_openapi::api::core::v1::ObjectReference;
use k8s_openapi::apimachinery::pkg::apis::meta::v1::ObjectMeta;
use k8s_openapi::apimachinery::pkg::util::intstr::IntOrString;

const JVM_UNITS: [&str; 4] = ["", "K", "M", "G"];
const K8S_UNITS: [&str; 7] = ["", "Ki", "Mi", "Gi", "Ti", "Pi", "Ei"];
const BASE_SI: u64 = 1000;
const BASE_BI: u64 = 1024;

#[inline]
fn format_mem(bytes: u64, base: u64, units: &[&str]) -> String {
    let mut unit = 0;
    let mut bytes = bytes;
    while bytes >= base && bytes % base == 0 && unit < units.len() {
        bytes /= base;
        unit += 1;
    }
    format!("{}{}", bytes, units[unit])
}

/// Converts bytes into the best machting jvm compatible size
/// It tries the largest possible suffix without loosing precision.
/// Means 1,5GB will be converted to 1536M
///
/// Also see [bytes_to_k8s_size] for a k8s compatible conversion
pub fn bytes_to_jvm_x_limit(bytes: u64) -> String {
    format_mem(bytes, BASE_BI, &JVM_UNITS)
}

/// Parses and int or string into bytes
/// If not possible, it will return None
pub fn parse_memory_spec(spec: &IntOrString) -> Option<u64> {
    match spec {
        IntOrString::Int(int) => Some(*int as u64),
        IntOrString::String(string) => string.parse::<ByteSize>().ok().map(|bs| bs.as_u64()),
    }
}

/// Parses and int or string into bytes.
/// If a percentage is given, it will be calculated relative to the base.
///  `base * percentage / 100`
/// If not possible, it will return None
pub fn parse_memoty_spec_with_relative_support(spec: &IntOrString, base: u64) -> Option<u64> {
    match spec {
        IntOrString::Int(int) => Some(*int as u64),
        IntOrString::String(string) => {
            if string.ends_with('%') {
                let percentage = string.trim_end_matches('%').parse::<f64>().ok()?;
                Some((base as f64 * percentage / 100.0) as u64)
            } else {
                string.parse::<ByteSize>().ok().map(|bs| bs.as_u64())
            }
        }
    }
}

#[inline]
pub fn object_ref_matches(
    object_ref: &ObjectReference,
    metadata: &ObjectMeta,
    api_version: Option<String>,
    kind: Option<String>,
) -> bool {
    //At least name or uid must exist and match.
    //If both are set, both must match.
    //All other fields are optional but must match if set

    if let Some(ref_namespace) = &object_ref.namespace {
        if let Some(met_namespace) = &metadata.namespace {
            if ref_namespace != met_namespace {
                return false;
            }
        }
    }

    if let Some(ref_api_version) = &object_ref.api_version {
        if let Some(met_api_version) = &api_version {
            if ref_api_version == met_api_version {
                return false;
            }
        }
    }
    if let Some(ref_kind) = &object_ref.kind {
        if let Some(met_kind) = &kind {
            if ref_kind == met_kind {
                return false;
            }
        }
    }

    let mut got_match = false;
    if let Some(ref_name) = &object_ref.name {
        if let Some(met_name) = &metadata.name {
            if ref_name == met_name {
                got_match = true;
            } else {
                return false;
            }
        }
    }
    if let Some(ref_uid) = &object_ref.uid {
        if let Some(met_uid) = &metadata.uid {
            if ref_uid == met_uid {
                got_match = true;
            } else {
                return false;
            }
        }
    }
    got_match
}
