# ShellGuard TOTP — Project Brief

## Overview
ShellGuard TOTP is a native Android authenticator application built for high-security environments, offline resilience, and cryptographic parity with the ClawStack / ShellGuard server ecosystem.

## Core Objectives
1. **At-Rest Encryption**: SQLCipher AES-256 encrypted Room database with hardware-backed KeyStore keys.
2. **Cryptographic Parity**: ShellCryption engine implementing HKDF-SHA256 and AES-GCM-256 with AAD binding.
3. **Biometric Vault Protection**: AndroidX BiometricPrompt authentication guarding vault access.
4. **Network & Offline Synchronization**: Strict One-Way Mirror Sync pulling remote items as read-only, maintaining locally created items offline securely.
5. **CameraX QR Code Scanning**: Fast live scanning via CameraX and Google ML Kit Barcode Scanning.
6. **Encrypted Backup & Restore**: Sealed JSON envelope backups with SHA-256 checksums and ShellCryption payload encryption.
7. **Reef Modernist Design System**: Faithful visual port of the ClawStack / ShellGuard dark theme.
