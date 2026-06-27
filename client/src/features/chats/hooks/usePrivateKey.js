import { useState, useEffect } from 'react';
import { getPrivateKey, savePrivateKey, deletePrivateKey } from '@/utils/indexedDBKeyStore';

export function usePrivateKey() {
    const [privateKey, setPrivateKey] = useState(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const loadKey = async () => {
            const key = await getPrivateKey();
            setPrivateKey(key);
            setLoading(false);
        };
        loadKey();
    }, []);

    const storeKey = async (key) => {
        await savePrivateKey(key);
        setPrivateKey(key);
    };

    const removeKey = async () => {
        await deletePrivateKey();
        setPrivateKey(null);
    };

    return {
        privateKey,
        isIndexDbLoading: loading,
        storePrivateKey: storeKey,
        deletePrivateKey: removeKey,
    };
}
