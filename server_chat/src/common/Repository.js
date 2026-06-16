const { Op } = require('sequelize');
const fs = require('fs');
const path = require('path');

const sequelize = require('../config/SequelizeConfig');
const { reduceImageBySize } = require('../utils/ImageUtils');
const { uploadToCloudinary } = require('../config/CloudinaryConfig');
const { FOUND, NOT_FOUND } = require('../utils/Messages');
const RuntimeError = require("./RuntimeError");
const ApiResponse = require("./ApiResponse");
const TABLE_PAGINATION_SIZE = 24;
const MAX_VIDEO_CHUNK_SIZE = 1024 * 1024;

const findById = async (model, id) => {
    const entity = await model.findByPk(id);

    if (!entity) {
        throw new RuntimeError(404, NOT_FOUND);
    }

    return entity;
}

const findAllByPagination = async (model, options = {}) => {
    const {
        page = 1,
        limit = TABLE_PAGINATION_SIZE,
        search = null,
        searchFields = [],
        include = [],
        attributes = null,
        group = undefined,
        order = [['createdAt', 'DESC']],
    } = options

    const filters = typeof options?.filters === 'string' ? JSON.parse(options?.filters) : options?.filters;
    const offset = (page - 1) * limit;

    const searchCondition = search && searchFields.length > 0 ?
        {
            [Op.or]: searchFields.map(field => ({
                [field]: {
                    [Op.iLike]: `%${search}%`
                }
            }))
        } : {};

    const where = {
        ...filters, ...searchCondition
    }

    const result = await model.findAndCountAll({
        where,
        limit,
        offset,
        order,
        include,
        ...(attributes && { attributes }),
        ...(group && { group }),
        distinct: true, // ensures proper count when using include/group
    });

    const totalPage = Math.ceil(result.count / limit);
    return {
        rows: result.rows,
        totalPages: totalPage >= 1 ? totalPage : 1,
    };
}

const create = async (model, reqBody) => {
    const transaction = await sequelize.transaction();

    try {
        const entity = await model.create({ ...reqBody }, { transaction });

        await transaction.commit();
        return entity;
    } catch (error) {
        await transaction.rollback();
        throw error;
    }
}

const update = async (model, id, reqBody) => {
    const transaction = await sequelize.transaction();

    try {
        const entity = await model.findByPk(id, { transaction });
        if (!entity) {
            throw new RuntimeError(404, NOT_FOUND);
        }

        entity.set(reqBody);
        await entity.save({ transaction });

        await transaction.commit();
        return entity;
    } catch (error) {
        await transaction.rollback();
        throw error;
    }
}

const remove = async (model, id) => {
    const transaction = await sequelize.transaction();

    try {
        const deletedCount = await model.destroy({
            where: { id },
            transaction
        });

        if (deletedCount === 0) {
            throw new RuntimeError(404, NOT_FOUND);
        }

        await transaction.commit();
    } catch (error) {
        await transaction.rollback();
        throw error;
    }
}

const uploadImage = async (image) => {
    if (!image) {
        throw RuntimeError(500, "Image not uploaded");
    }

    const reducedBuffer = await reduceImageBySize(image.buffer);
    const uploaded = await uploadToCloudinary(reducedBuffer);
    const imageUrl = uploaded.secure_url;
    // console.log("image url: ", imageUrl);
    return ApiResponse({message: FOUND, data: imageUrl})
}

const streamVideo = async (filename, range) => {
    //console.log("1");
    const filePath = path.join(__dirname, '../../videos', filename);

    if (!fs.existsSync(filePath)) {
        //console.log("2");
        throw new RuntimeError(404, 'VIDEO_NOT_FOUND');
    }

    //console.log("3" + range);
    const stat = fs.statSync(filePath);
    const fileSize = stat.size;

    //console.log("4" + range);
    const parts = range.replace(/bytes=/, '').split('-');
    const start = parseInt(parts[0], 10);
    const end = parts[1] ? parseInt(parts[1], 10) : Math.min(start + MAX_VIDEO_CHUNK_SIZE, fileSize) - 1;
    //const end = parts[1] ? parseInt(parts[1], 10) : fileSize -1;
    const chunkSize = end - start + 1;

    const stream = fs.createReadStream(filePath, { start, end });

    return {
        statusCode: 206,
        headers: {
            'Content-Range': `bytes ${start}-${end}/${fileSize}`,
            'Accept-Ranges': 'bytes',
            'Content-Length': chunkSize,
            'Content-Type': 'video/webm',
        },
        stream,
    };
};

module.exports = {
    findById,
    findAllByPagination,
    create,
    update,
    remove,
    uploadImage,
    streamVideo,
}