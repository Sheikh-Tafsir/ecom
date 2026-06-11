const { IS_REQUIRED } = require('../utils/Messages');
const { UserStatus } = require('../utils/Enum');

module.exports = (sequelize, DataTypes) => {
    const User = sequelize.define('User',
        {
            name: {
                type: DataTypes.STRING,
                allowNull: false,
                validate: {
                    notNull: { msg: IS_REQUIRED },
                }
            },

            email: {
                type: DataTypes.STRING,
                allowNull: false,
                unique: true,
                validate: {
                    notNull: { msg: IS_REQUIRED },
                    isEmail: true,
                }
            },

            password: {
                type: DataTypes.STRING,
                allowNull: false,
            },

            image: {
                type: DataTypes.TEXT,
                allowNull: true,
            },

            phone: {
                type: DataTypes.STRING,
                allowNull: true,
                unique: true,
            },

            gender: {
                type: DataTypes.STRING,
                allowNull: true,
            },

            status: {
                type: DataTypes.ENUM(...Object.values(UserStatus)),
                allowNull: false,
                defaultValue: UserStatus.INACTIVE,
            },

            deleted: {
                type: DataTypes.BOOLEAN,
                defaultValue: false,
            },

            createdAt: {
                type: DataTypes.DATE,
                allowNull: false,
                defaultValue: DataTypes.NOW,
                field: 'created_at'
            },

            updatedAt: {
                type: DataTypes.DATE,
                allowNull: false,
                defaultValue: DataTypes.NOW,
                onUpdate: DataTypes.NOW,
                field: 'updated_at'
            }
        },
        {
            tableName: 'product_users',
            timestamps: true,
            createdAt: 'createdAt',
            updatedAt: 'updatedAt',
        }
    );

    User.associate = models => {
        User.hasMany(models.ChatParticipant, { foreignKey: 'userId', as: 'Participations' });
        User.hasMany(models.Message, { foreignKey: 'senderId', as: 'SentMessages' });
    };

    return User;
};
