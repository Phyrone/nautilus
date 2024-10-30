plugins {
    `kotlin-dsl`
}
repositories{
    mavenCentral()
}

dependencies{
    implementation("org.bouncycastle:bcprov-jdk18on:1.79")
    implementation("org.bouncycastle:bcpkix-jdk18on:1.79")
}