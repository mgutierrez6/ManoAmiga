// Los listados aceptan page y limit (máximo 50) y devuelven { data, page, total } (3.1).
async function paginar(query, countQuery, { page = 1, limit = 20 }) {
  const skip = (page - 1) * limit;
  const [data, total] = await Promise.all([query.skip(skip).limit(limit), countQuery]);
  return { data, page, limit, total };
}

module.exports = { paginar };
