/******************************************************************************* 
 * Copyright (c) 2013 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package com.openshift.internal.client;

import static com.openshift.client.utils.FileUtils.createRandomTempFile;
import static org.fest.assertions.Assertions.assertThat;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.openshift.client.IOpenShiftSSHKey;
import com.openshift.client.ISSHPublicKey;
import com.openshift.client.IUser;
import com.openshift.client.OpenShiftEndpointException;
import com.openshift.client.OpenShiftSSHKeyException;
import com.openshift.client.SSHKeyPair;
import com.openshift.client.SSHKeyType;
import com.openshift.client.SSHPublicKey;
import com.openshift.client.utils.SSHKeyTestUtils;
import com.openshift.client.utils.SSHPublicKeyAssertion;
import com.openshift.client.utils.TestConnectionBuilder;
import com.openshift.internal.client.httpclient.HttpClientException;

/**
 * @author Andre Dietisheim
 */
public class SSHKeyIntegrationTest extends TestTimer {

	private IUser user;

	@Before
	public void setUp() throws Exception {
		this.user = new TestConnectionBuilder().defaultCredentials().disableSSLCertificateChecks().create().getUser();
	}

	@Test
	public void shouldReturnExistingKeys() throws HttpClientException, Throwable {
		// pre-conditions
		// operation
		List<IOpenShiftSSHKey> sshKeys = user.getSSHKeys();
		// verifications
		assertThat(sshKeys).isNotNull();
	}

	@Test
	public void shouldAddKey() throws Exception {
		IOpenShiftSSHKey key = null;
		try {
			// pre-conditions
			String publicKeyPath = SSHKeyTestUtils.createDsaKeyPair();
			ISSHPublicKey publicKey = new SSHPublicKey(publicKeyPath);
			int numOfKeys = user.getSSHKeys().size();

			// operation
			String keyName = SSHKeyTestUtils.createRandomKeyName();
			key = user.addSSHKey(keyName, publicKey);

			// verifications
			assertThat(
					new SSHPublicKeyAssertion(key))
					.hasName(keyName)
					.hasPublicKey(publicKey.getPublicKey())
					.isType(publicKey.getKeyType());
			List<IOpenShiftSSHKey> keys = user.getSSHKeys();
			assertThat(keys.size()).isEqualTo(numOfKeys + 1);
			IOpenShiftSSHKey keyInList = SSHKeyTestUtils.getKey(keyName, keys);
			assertThat(key).isEqualTo(keyInList);
		} finally {
			SSHKeyTestUtils.silentlyDestroyKey(key);
		}
	}

	@Test(expected=OpenShiftSSHKeyException.class)
	public void shouldNotAddKeyTwice() throws Exception {
		IOpenShiftSSHKey key = null;
		try {
			// pre-conditions
			String keyName = SSHKeyTestUtils.createRandomKeyName();
			ISSHPublicKey publicKey = new SSHPublicKey(SSHKeyTestUtils.createDsaKeyPair());
			key = user.addSSHKey(keyName, publicKey);
			
			// operation
			key = user.addSSHKey(keyName, publicKey);

			// verifications
		} finally {
			SSHKeyTestUtils.silentlyDestroyKey(key);
		}
	}
	
	@Test(expected=OpenShiftEndpointException.class)
	public void shouldNotPutKeyTwice() throws Exception {
		IOpenShiftSSHKey key = null;
		try {
			// pre-conditions
			String keyName = SSHKeyTestUtils.createRandomKeyName();
			ISSHPublicKey publicKey = new SSHPublicKey(SSHKeyTestUtils.createDsaKeyPair());
			key = user.putSSHKey(keyName, publicKey);
			
			// operation
			key = user.putSSHKey(keyName, publicKey);

			// verifications
		} finally {
			SSHKeyTestUtils.silentlyDestroyKey(key);
		}
	}
	
	@Test
	public void shouldUpdatePublicKey() throws Exception {
		IOpenShiftSSHKey key = null;
		try {
			// pre-conditions
			String publicKeyPath = createRandomTempFile().getAbsolutePath();
			String privateKeyPath = createRandomTempFile().getAbsolutePath();
			SSHKeyPair keyPair = SSHKeyPair.create(
					SSHKeyType.SSH_RSA,
					SSHKeyTestUtils.DEFAULT_PASSPHRASE,
					privateKeyPath,
					publicKeyPath);
			String keyName = SSHKeyTestUtils.createRandomKeyName();
			key = user.addSSHKey(keyName, keyPair);

			// operation
			String publicKey = SSHKeyPair.create(
					SSHKeyType.SSH_RSA,
					SSHKeyTestUtils.DEFAULT_PASSPHRASE,
					privateKeyPath,
					publicKeyPath).getPublicKey();
			key.setPublicKey(publicKey);

			// verification
			assertThat(key.getPublicKey()).isEqualTo(publicKey);
			IOpenShiftSSHKey openshiftKey = user.getSSHKeyByName(keyName);
			assertThat(
					new SSHPublicKeyAssertion(openshiftKey))
					.hasName(keyName)
					.hasPublicKey(publicKey)
					.isType(openshiftKey.getKeyType());
		} finally {
			SSHKeyTestUtils.silentlyDestroyKey(key);
		}

	}

	@Test
	public void shouldReturnKeyForName() throws Exception {
		IOpenShiftSSHKey key = null;
		try {
			// pre-conditions
			String publicKeyPath = SSHKeyTestUtils.createDsaKeyPair();
			ISSHPublicKey publicKey = new SSHPublicKey(publicKeyPath);

			// operation
			String keyName = SSHKeyTestUtils.createRandomKeyName();
			key = user.addSSHKey(keyName, publicKey);
			IOpenShiftSSHKey keyByName = user.getSSHKeyByName(keyName);

			// verifications
			assertThat(key).isEqualTo(keyByName);
		} finally {
			SSHKeyTestUtils.silentlyDestroyKey(key);
		}
	}

	@Test
	public void shouldReturnKeyForPublicKey() throws Exception {
		IOpenShiftSSHKey key = null;
		try {
			// pre-conditions
			String publicKeyPath = SSHKeyTestUtils.createDsaKeyPair();
			ISSHPublicKey publicKey = new SSHPublicKey(publicKeyPath);

			// operation
			String keyName = SSHKeyTestUtils.createRandomKeyName();
			key = user.addSSHKey(keyName, publicKey);
			IOpenShiftSSHKey keyByPublicKey = user.getSSHKeyByPublicKey(publicKey.getPublicKey());

			// verifications
			assertThat(key).isEqualTo(keyByPublicKey);
		} finally {
			SSHKeyTestUtils.silentlyDestroyKey(key);
		}

	}

	@Test
	public void shouldUpdateKeyTypeAndPublicKey() throws Exception {
		IOpenShiftSSHKey key = null;
		try {
			// pre-conditions
			String publicKeyPath = createRandomTempFile().getAbsolutePath();
			String privateKeyPath = createRandomTempFile().getAbsolutePath();
			SSHKeyTestUtils.createDsaKeyPair(publicKeyPath, privateKeyPath);
			ISSHPublicKey publicKey = new SSHPublicKey(publicKeyPath);
			assertThat(publicKey.getKeyType()).isEqualTo(SSHKeyType.SSH_DSA);
			String keyName = SSHKeyTestUtils.createRandomKeyName();
			key = user.addSSHKey(keyName, publicKey);
			SSHKeyPair keyPair = SSHKeyPair.create(
					SSHKeyType.SSH_RSA, SSHKeyTestUtils.DEFAULT_PASSPHRASE, privateKeyPath, publicKeyPath);

			// operation
			key.setKeyType(SSHKeyType.SSH_RSA, keyPair.getPublicKey());

			// verification
			assertThat(key.getKeyType()).isEqualTo(SSHKeyType.SSH_RSA);
			assertThat(key.getPublicKey()).isEqualTo(keyPair.getPublicKey());
		} finally {
			SSHKeyTestUtils.silentlyDestroyKey(key);
		}
	}

	@Test
	public void shouldDestroyKey() throws Exception {
		IOpenShiftSSHKey key = null;
		try {
			// pre-conditions
			String publicKeyPath = SSHKeyTestUtils.createDsaKeyPair();
			String keyName = SSHKeyTestUtils.createRandomKeyName();
			key = user.addSSHKey(keyName, new SSHPublicKey(publicKeyPath));
			
			// operation
			key.destroy();
			key = null;
			
			// verification
			assertThat(user.getSSHKeyByName(keyName)).isNull();
		} finally {
			SSHKeyTestUtils.silentlyDestroyKey(key);
		}
	}
	
	@Test
	public void shouldRemoveKeyByName() throws Exception {
		IOpenShiftSSHKey key = null;
		try {
			// pre-conditions
			String publicKeyPath = SSHKeyTestUtils.createDsaKeyPair();
			String keyName = SSHKeyTestUtils.createRandomKeyName();
			key = user.addSSHKey(keyName, new SSHPublicKey(publicKeyPath));
			int numOfKeys = user.getSSHKeys().size();
			
			// operation
			user.deleteKey(keyName);
			
			// verification
			assertThat(user.getSSHKeyByName(keyName)).isNull();
			assertThat(user.getSSHKeys().size()).isEqualTo(numOfKeys -1);
		} finally {
			SSHKeyTestUtils.silentlyDestroyKey(key);
		}
	}

	@Test
	public void shouldRefreshKeys() throws Exception {
		IOpenShiftSSHKey key = null;
		int originalNumOfKeys = user.getSSHKeys().size();
		try {
			// pre-conditions
			String publicKeyPath = SSHKeyTestUtils.createDsaKeyPair();
			IUser user = new TestConnectionBuilder().defaultCredentials().disableSSLCertificateChecks().create().getUser();
			
			// operation
			int newNumOfKeys = user.getSSHKeys().size();
			assertThat(user.getSSHKeys().size()).isEqualTo(originalNumOfKeys);
			String newKeyName = SSHKeyTestUtils.createRandomKeyName();
			user.addSSHKey(newKeyName, new SSHPublicKey(publicKeyPath));			
			newNumOfKeys = user.getSSHKeys().size();
			
			// verification
			assertThat(newNumOfKeys).isEqualTo(originalNumOfKeys + 1);
		} finally {
			SSHKeyTestUtils.silentlyDestroyKey(key);
		}
	}
}
