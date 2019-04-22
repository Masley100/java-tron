package org.tron.common.zksnark.zen.note;

import static org.tron.common.zksnark.zen.Libsodium.crypto_aead_chacha20poly1305_IETF_NPUBBYTES;
import static org.tron.common.zksnark.zen.note.NoteEncryption.NOTEENCRYPTION_CIPHER_KEYSIZE;

import java.util.Optional;
import lombok.AllArgsConstructor;
import org.tron.common.zksnark.zen.Librustzcash;
import org.tron.common.zksnark.zen.Libsodium;
import org.tron.common.zksnark.zen.ZkChainParams;
import org.tron.common.zksnark.zen.address.DiversifierT;
import org.tron.common.zksnark.zen.note.NoteEncryption.EncCiphertext;
import org.tron.common.zksnark.zen.note.NoteEncryption.EncPlaintext;
import org.tron.common.zksnark.zen.note.NoteEncryption.OutCiphertext;
import org.tron.common.zksnark.zen.note.NoteEncryption.OutPlaintext;
import org.tron.common.zksnark.zen.transaction.Ciphertext;

@AllArgsConstructor
public class SaplingNoteEncryption {

  // Ephemeral public key
  public byte[] epk;

  // Ephemeral secret key
  public byte[] esk;

  public boolean already_encrypted_enc;
  public boolean already_encrypted_out;

  public SaplingNoteEncryption(byte[] epk, byte[] esk) {
    this.epk = epk;
    this.esk = esk;
  }

  //todo:
  public static Optional<SaplingNoteEncryption> fromDiversifier(DiversifierT d) {
    byte[] epk = new byte[32];
    byte[] esk = new byte[32];
    Librustzcash.librustzcashSaplingGenerateR(esk);
    if (!Librustzcash.librustzcashSaplingKaDerivepublic(d.data, esk, epk)) {
      return Optional.empty();
    }

    return Optional.of(new SaplingNoteEncryption(epk, esk));

  }

  Optional<EncCiphertext> encryptToRecipient(byte[] pk_d, EncPlaintext message) {
    if (already_encrypted_enc) {
      throw new RuntimeException("already encrypted to the recipient using this key");
    }

    byte[] dhsecret = new byte[32];
    if (!Librustzcash.librustzcashSaplingKaAgree(pk_d, esk, dhsecret)) {
      return Optional.empty();
    }

    byte[] k = new byte[NOTEENCRYPTION_CIPHER_KEYSIZE];
    NoteEncryption.KDFSapling(k, dhsecret, epk);
    byte[] cipherNonce = new byte[crypto_aead_chacha20poly1305_IETF_NPUBBYTES];
    EncCiphertext ciphertext = new EncCiphertext();
    Libsodium.cryptoAeadChacha20Poly1305IetfEncrypt(ciphertext.data, null, message.data,
        ZkChainParams.ZC_SAPLING_ENCPLAINTEXT_SIZE, null, 0, null, cipherNonce, k);
    already_encrypted_enc = true;
    return Optional.of(ciphertext);
  }

  OutCiphertext encrypt_to_ourselves(
      byte[] ovk, byte[] cv, byte[] cm, OutPlaintext message) {
    return null;
  }
}
