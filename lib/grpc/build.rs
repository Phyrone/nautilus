fn main() {
    println!("cargo:rerun-if-changed=../../proto");

    let builder = tonic_build::configure();
    let builder = builder.compile_well_known_types(true)
        .use_arc_self(true)
        .build_client(true)
        .build_server(true)
        .build_transport(true)
        .emit_rerun_if_changed(false)
        .include_file("_all.rs")
        //makes it easier to inspect the generated code it gets ignored by git
        .out_dir("src/main/rust/");

    //get proto files from ../../proto
    let files = walkdir::WalkDir::new("../../proto")
        .into_iter()
        .filter_map(|e| e.ok())
        .filter(|e| e.path().extension().map_or(false, |e| e == "proto"))
        .map(|e| e.path().to_path_buf())
        .collect::<Vec<_>>();


    builder.compile_protos(
        &files,
        &["../../proto"],
    ).expect("Failed to compile protos");
}