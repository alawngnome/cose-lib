/*
 * Copyright 2022 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cose;

import co.nstant.in.cbor.CborException;
import co.nstant.in.cbor.model.Array;
import co.nstant.in.cbor.model.ByteString;
import co.nstant.in.cbor.model.DataItem;
import co.nstant.in.cbor.model.MajorType;
import co.nstant.in.cbor.model.Map;
import co.nstant.in.cbor.model.NegativeInteger;
import co.nstant.in.cbor.model.UnsignedInteger;
import com.google.cose.exceptions.CoseException;
import com.google.cose.utils.Algorithm;
import com.google.cose.utils.CborUtils;
import com.google.cose.utils.Headers;
import java.util.LinkedHashSet;
import java.util.Set;

/** Implements COSE_Key spec for encryption purposes. */
public final class EncryptionKey extends CoseKey {
  private final byte[] secretKey;

  public EncryptionKey(final DataItem cborKey) throws CborException, CoseException {
    super(cborKey);
    if (labels.containsKey(Headers.KEY_PARAMETER_K)
        && labels.get(Headers.KEY_PARAMETER_K).getMajorType() == MajorType.BYTE_STRING) {
      byte[] keyMaterial = CborUtils.asByteString(labels.get(Headers.KEY_PARAMETER_K)).getBytes();
      if (keyMaterial.length == 0) {
        throw new CoseException("Missing key material information.");
      } else {
        secretKey = keyMaterial;
      }
    } else {
      throw new CoseException("Missing key material information.");
    }

    if ((operations != null)
        && !operations.contains(Headers.KEY_OPERATIONS_DECRYPT)
        && !operations.contains(Headers.KEY_OPERATIONS_ENCRYPT)) {
      throw new CoseException("Encryption key requires either encrypt or decrypt operation.");
    }
  }

  static class Builder {
    private String keyId;
    private Algorithm algorithm;
    private final Set<Integer> operations = new LinkedHashSet<>();
    private byte[] baseIv;
    private byte[] secretKey;

    public EncryptionKey build() throws CoseException, CborException {
      if (secretKey == null) {
        throw new CoseException("Missing key material information.");
      }

      Map cborKey = new Map();
      cborKey.put(new UnsignedInteger(Headers.KEY_PARAMETER_KEY_TYPE),
          new UnsignedInteger(Headers.KEY_TYPE_SYMMETRIC));

      if (keyId != null) {
        cborKey.put(new UnsignedInteger(Headers.KEY_PARAMETER_KEY_ID),
            new ByteString(keyId.getBytes()));
      }
      if (algorithm != null) {
        cborKey.put(new UnsignedInteger(Headers.KEY_PARAMETER_ALGORITHM),
            algorithm.getCoseAlgorithmId());
      }
      if (operations.size() != 0) {
        Array keyOperations = new Array();
        for (int operation: operations) {
          keyOperations.add(new UnsignedInteger(operation));
        }
        cborKey.put(new UnsignedInteger(Headers.KEY_PARAMETER_OPERATIONS), keyOperations);
      }
      if (baseIv != null) {
        cborKey.put(new UnsignedInteger(Headers.KEY_PARAMETER_BASE_IV),
            new ByteString(baseIv));
      }
      if (secretKey != null) {
        cborKey.put(new NegativeInteger(Headers.KEY_PARAMETER_K), new ByteString(secretKey));
      }
      return new EncryptionKey(cborKey);
    }

    public Builder withKeyId(String keyId) {
      this.keyId = keyId;
      return this;
    }

    public Builder withAlgorithm(Algorithm algorithm) {
      this.algorithm = algorithm;
      return this;
    }

    public Builder withOperations(Integer...operations) throws CoseException {
      for (int operation : operations) {
        if (operation != Headers.KEY_OPERATIONS_ENCRYPT && operation != Headers.KEY_OPERATIONS_DECRYPT)
          throw new CoseException("Encryption key only supports Encrypt or Decrypt operations.");
        this.operations.add(operation);
      }
      return this;
    }

    public Builder withBaseIv(byte[] baseIv) {
      this.baseIv = baseIv;
      return this;
    }

    public Builder withSecretKey(byte[] k) {
      this.secretKey = k;
      return this;
    }
  }

  public static Builder builder() {
    return new Builder();
  }

  public static EncryptionKey parse(byte[] keyBytes) throws CborException, CoseException {
    DataItem dataItem = CborUtils.decode(keyBytes);
    return decode(dataItem);
  }

  public static EncryptionKey decode(DataItem cborKey) throws CborException, CoseException {
    return new EncryptionKey(cborKey);
  }
}